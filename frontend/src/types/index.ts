/**
 * Type definitions for AgentCFO
 */

export type DocumentType = 
  | "invoice"
  | "receipt"
  | "contract"
  | "letter"
  | "tax_document"
  | "insurance"
  | "other";

export type DocumentStatus = "uploading" | "processing" | "completed" | "failed";

export interface Document {
  id: number;
  user_id: number;
  filename: string;
  original_filename: string;
  file_size: number;
  mime_type: string;
  document_type: DocumentType;
  status: DocumentStatus;
  extracted_text?: string;
  language?: string;
  num_pages?: number;
  created_at: string;
  updated_at: string;
}

export interface ChatMessage {
  id?: number;
  role: "user" | "assistant" | "system";
  content: string;
  created_at?: string;
}

export interface Conversation {
  id: number;
  agent_type: "accountant" | "legal";
  title?: string;
  created_at: string;
  updated_at: string;
  message_count: number;
}

