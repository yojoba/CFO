"""Document analysis agent for intelligent document processing."""
from typing import Dict, Any, Optional
from datetime import datetime, date
from openai import AsyncOpenAI
from loguru import logger
import json
import re

from app.config import settings


class DocumentAgent:
    """Agent specialized in document analysis and classification."""
    
    SYSTEM_PROMPT = """Tu es un assistant expert en analyse de documents pour les ménages suisses.

Ton rôle est d'analyser le contenu de documents (factures, lettres, contrats, reçus) et d'en extraire des métadonnées structurées.

Tu dois extraire et retourner UNIQUEMENT un objet JSON valide avec les champs suivants:
{
  "document_type": "invoice|letter|contract|receipt|other",
  "category": "Thème principal du document",
  "display_name": "Titre court descriptif du document",
  "document_date": "YYYY-MM-DD ou null",
  "deadline": "YYYY-MM-DD ou null",
  "amount": nombre ou null,
  "currency": "CHF|EUR|USD ou null",
  "keywords": ["mot-clé1", "mot-clé2", ...],
  "importance_factors": {
    "has_deadline": true/false,
    "is_urgent": true/false,
    "has_high_amount": true/false,
    "requires_action": true/false
  },
  "confidence": 0.0-1.0,
  "summary": "Brève description du document en français"
}

Règles d'extraction:
1. document_type: Classifie précisément le type de document (invoice, letter, contract, receipt, other)

2. category: Thème/catégorie principale du document. Exemples:
   - "Impots" : impôts cantonaux, fédéraux, déclarations fiscales
   - "Poursuites" : commandements de payer, poursuites, contentieux
   - "Assurance" : assurance maladie, véhicule, habitation
   - "Banque" : relevés bancaires, cartes de crédit
   - "Energie" : électricité, gaz, eau
   - "Telecom" : téléphone, internet, TV
   - "Sante" : médecin, hôpital, pharmacie, dentiste
   - "Immobilier" : loyer, charges, entretien
   - "Emploi" : salaire, contrat de travail
   - "General" : si aucun thème spécifique (par défaut)
   
3. display_name: Titre court et descriptif (30-50 caractères MAX) qui identifie clairement le document
   IMPORTANT: NE PAS inclure le montant dans le titre (il est déjà extrait séparément)
   
   Exemples CORRECTS:
   - "Commandement de payer - Office cantonal"
   - "Facture Électricité Romande Energie - Nov 2024"
   - "Courrier Impôts Canton du Valais 2024"
   - "Contrat Location Appartement Rue du Pont"
   - "Reçu Achat Migros"
   - "Poursuite - Etat du Valais 2024"
   
   Format recommandé: [Type Document] [Émetteur] [Sujet/Période]
   NE PAS ajouter de montant, il sera affiché séparément!

4. document_date: Date du document (date d'émission, pas la date de réception)
5. deadline: Date limite de paiement, de réponse, ou d'action requise
6. amount: Montant principal (total à payer pour factures, montant du contrat, etc.)
7. currency: Devise du montant
8. keywords: 5-10 mots-clés importants (noms propres, références, sujets principaux)
9. importance_factors:
   - has_deadline: true si une date limite existe
   - is_urgent: true si mots comme "urgent", "rappel", "dernière chance", "mise en demeure"
   - has_high_amount: true si montant > 500 CHF
   - requires_action: true si une action est requise (payer, répondre, signer, etc.)
10. confidence: Ton niveau de confiance dans l'analyse (0.0 à 1.0)
11. summary: Résumé en 1-2 phrases du contenu et de l'action requise si applicable

Dates:
- Cherche "date d'échéance", "à payer avant le", "délai de paiement", "date limite"
- Formats courants: DD.MM.YYYY, DD/MM/YYYY, YYYY-MM-DD
- Si année manquante, utilise l'année courante ou suivante selon le contexte

Montants:
- Cherche "Total", "Montant à payer", "Solde dû", "Prix"
- Formats: 1'234.50, 1234.50, 1 234,50

IMPORTANT: Réponds UNIQUEMENT avec le JSON, sans texte avant ou après."""

    def __init__(self):
        self.client = AsyncOpenAI(api_key=settings.OPENAI_API_KEY)
    
    async def analyze_document(
        self, 
        extracted_text: str,
        ocr_confidence: float = 1.0
    ) -> Dict[str, Any]:
        """
        Analyze document content and extract structured metadata.
        
        Args:
            extracted_text: OCR extracted text from document
            ocr_confidence: Confidence score from OCR (0.0-1.0)
            
        Returns:
            Dict with structured document metadata
        """
        try:
            if not extracted_text or not extracted_text.strip():
                logger.warning("Empty text provided for analysis")
                return self._get_default_metadata()
            
            # Call OpenAI API for analysis
            response = await self.client.chat.completions.create(
                model=settings.OPENAI_MODEL,
                messages=[
                    {"role": "system", "content": self.SYSTEM_PROMPT},
                    {"role": "user", "content": f"Analyse ce document:\n\n{extracted_text[:4000]}"}  # Limit to 4000 chars
                ],
                temperature=0.3,  # Lower temperature for more consistent extraction
                max_tokens=800
            )
            
            # Parse JSON response
            content = response.choices[0].message.content.strip()
            
            # Extract JSON from response (in case there's extra text)
            json_match = re.search(r'\{.*\}', content, re.DOTALL)
            if json_match:
                content = json_match.group(0)
            
            metadata = json.loads(content)
            
            # Validate and normalize the response
            metadata = self._validate_metadata(metadata, ocr_confidence)
            
            logger.info(f"Successfully analyzed document: type={metadata.get('document_type')}, confidence={metadata.get('confidence')}")
            return metadata
            
        except json.JSONDecodeError as e:
            logger.error(f"Failed to parse JSON from LLM response: {e}")
            logger.debug(f"Response content: {content}")
            return self._get_default_metadata()
        except Exception as e:
            logger.error(f"Error analyzing document: {e}")
            return self._get_default_metadata()
    
    def _validate_metadata(self, metadata: Dict[str, Any], ocr_confidence: float) -> Dict[str, Any]:
        """
        Validate and normalize extracted metadata.
        
        Args:
            metadata: Raw metadata from LLM
            ocr_confidence: OCR confidence score
            
        Returns:
            Validated and normalized metadata
        """
        # Ensure all required fields exist
        validated = {
            'document_type': metadata.get('document_type', 'other'),
            'category': metadata.get('category', 'General'),  # Thème du document
            'display_name': metadata.get('display_name', '')[:80],  # Max 80 chars (plus court)
            'document_date': self._parse_date(metadata.get('document_date')),
            'deadline': self._parse_date(metadata.get('deadline')),
            'amount': self._parse_amount(metadata.get('amount')),
            'currency': metadata.get('currency', 'CHF'),
            'keywords': metadata.get('keywords', [])[:10],  # Max 10 keywords
            'importance_factors': metadata.get('importance_factors', {}),
            'confidence': min(metadata.get('confidence', 0.5), ocr_confidence),  # Limited by OCR confidence
            'summary': metadata.get('summary', '')[:500]  # Max 500 chars
        }
        
        # Ensure importance_factors has all fields
        validated['importance_factors'] = {
            'has_deadline': validated['importance_factors'].get('has_deadline', False),
            'is_urgent': validated['importance_factors'].get('is_urgent', False),
            'has_high_amount': validated['importance_factors'].get('has_high_amount', False),
            'requires_action': validated['importance_factors'].get('requires_action', False)
        }
        
        # Calculate importance score
        validated['importance_score'] = self._calculate_importance_score(validated)
        
        return validated
    
    def _parse_date(self, date_str: Any) -> Optional[str]:
        """
        Parse date string to ISO format (YYYY-MM-DD).
        
        Args:
            date_str: Date string or None
            
        Returns:
            ISO formatted date string or None
        """
        if not date_str or date_str == "null":
            return None
        
        if isinstance(date_str, date):
            return date_str.isoformat()
        
        if not isinstance(date_str, str):
            return None
        
        # Try to parse various date formats
        date_patterns = [
            r'(\d{4})-(\d{2})-(\d{2})',  # YYYY-MM-DD
            r'(\d{2})\.(\d{2})\.(\d{4})',  # DD.MM.YYYY
            r'(\d{2})/(\d{2})/(\d{4})',  # DD/MM/YYYY
        ]
        
        for pattern in date_patterns:
            match = re.search(pattern, date_str)
            if match:
                try:
                    if pattern == date_patterns[0]:  # YYYY-MM-DD
                        year, month, day = match.groups()
                    else:  # DD.MM.YYYY or DD/MM/YYYY
                        day, month, year = match.groups()
                    
                    parsed_date = date(int(year), int(month), int(day))
                    return parsed_date.isoformat()
                except ValueError:
                    continue
        
        return None
    
    def _parse_amount(self, amount: Any) -> Optional[float]:
        """
        Parse amount to float.
        
        Args:
            amount: Amount value (number, string, or None)
            
        Returns:
            Float amount or None
        """
        if amount is None or amount == "null":
            return None
        
        if isinstance(amount, (int, float)):
            return float(amount)
        
        if isinstance(amount, str):
            # Remove common formatting
            amount = amount.replace("'", "").replace(" ", "").replace(",", ".")
            try:
                return float(amount)
            except ValueError:
                return None
        
        return None
    
    def _calculate_importance_score(self, metadata: Dict[str, Any]) -> float:
        """
        Calculate importance score (0-100) based on multiple factors.
        
        Args:
            metadata: Document metadata
            
        Returns:
            Importance score (0-100)
        """
        score = 50.0  # Base score
        
        factors = metadata.get('importance_factors', {})
        
        # Deadline proximity (up to +30 points)
        if metadata.get('deadline'):
            try:
                deadline_date = datetime.fromisoformat(metadata['deadline']).date()
                days_until = (deadline_date - date.today()).days
                
                if days_until < 0:
                    score += 30  # Overdue!
                elif days_until <= 3:
                    score += 25
                elif days_until <= 7:
                    score += 20
                elif days_until <= 14:
                    score += 15
                elif days_until <= 30:
                    score += 10
            except (ValueError, TypeError):
                pass
        
        # Urgency markers (+15 points)
        if factors.get('is_urgent'):
            score += 15
        
        # High amount (+15 points)
        if factors.get('has_high_amount'):
            score += 15
        elif metadata.get('amount'):
            amount = metadata['amount']
            if amount > 1000:
                score += 15
            elif amount > 500:
                score += 10
            elif amount > 200:
                score += 5
        
        # Requires action (+10 points)
        if factors.get('requires_action'):
            score += 10
        
        # Adjust by confidence
        confidence = metadata.get('confidence', 0.5)
        score = score * (0.7 + 0.3 * confidence)  # Scale by confidence (70-100%)
        
        # Ensure score is in range [0, 100]
        return max(0.0, min(100.0, score))
    
    def _get_default_metadata(self) -> Dict[str, Any]:
        """
        Get default metadata structure when analysis fails.
        
        Returns:
            Default metadata dict
        """
        return {
            'document_type': 'other',
            'category': 'General',
            'display_name': 'Document sans titre',
            'document_date': None,
            'deadline': None,
            'amount': None,
            'currency': 'CHF',
            'keywords': [],
            'importance_factors': {
                'has_deadline': False,
                'is_urgent': False,
                'has_high_amount': False,
                'requires_action': False
            },
            'confidence': 0.0,
            'summary': 'Analyse automatique non disponible',
            'importance_score': 50.0
        }

