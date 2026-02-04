"""Image preprocessing service for document auto-crop and enhancement."""
import cv2
import numpy as np
from PIL import Image, ImageEnhance
from typing import Optional, Tuple
from loguru import logger
from pathlib import Path

from app.config import settings


class ImagePreprocessingService:
    """Service for preprocessing images before OCR: auto-crop, deskew, enhance."""
    
    def __init__(self):
        self.enable_auto_crop = getattr(settings, 'ENABLE_AUTO_CROP', True)
        self.enable_deskew = getattr(settings, 'ENABLE_DESKEW', True)
        self.enable_contrast = getattr(settings, 'ENABLE_CONTRAST_ENHANCEMENT', True)
        self.enable_denoise = getattr(settings, 'ENABLE_NOISE_REDUCTION', True)
        self.min_area_ratio = getattr(settings, 'MIN_DOCUMENT_AREA_RATIO', 0.1)
        self.deskew_threshold = getattr(settings, 'DESKEW_ANGLE_THRESHOLD', 0.5)
    
    def preprocess_for_ocr(self, image_path: str, output_path: str) -> Tuple[bool, str]:
        """
        Complete preprocessing pipeline for OCR optimization.
        
        Pipeline: Load → Auto-crop → Deskew → Enhance Contrast → Reduce Noise → Save
        
        Args:
            image_path: Path to original image
            output_path: Path to save preprocessed image
            
        Returns:
            Tuple of (success, final_path)
        """
        try:
            logger.info(f"Starting preprocessing pipeline for: {image_path}")
            
            # Load image
            img = cv2.imread(image_path)
            if img is None:
                logger.error(f"Failed to load image: {image_path}")
                return False, image_path
            
            original_shape = img.shape
            logger.info(f"Original image size: {original_shape[1]}x{original_shape[0]}")
            
            # Step 1: Auto-crop (detect and crop document)
            if self.enable_auto_crop:
                cropped_img = self.detect_and_crop_document(img)
                if cropped_img is not None:
                    img = cropped_img
                    logger.info(f"✓ Document cropped to: {img.shape[1]}x{img.shape[0]}")
                else:
                    logger.warning("Auto-crop failed, using original")
            
            # Step 2: Deskew (straighten rotated document)
            if self.enable_deskew:
                deskewed_img, angle = self.deskew_image(img)
                if deskewed_img is not None and abs(angle) > self.deskew_threshold:
                    img = deskewed_img
                    logger.info(f"✓ Document deskewed by {angle:.2f}°")
            
            # Step 3: Enhance contrast
            if self.enable_contrast:
                img = self.enhance_contrast(img)
                logger.info("✓ Contrast enhanced")
            
            # Step 4: Reduce noise
            if self.enable_denoise:
                img = self.reduce_noise(img)
                logger.info("✓ Noise reduced")
            
            # Save preprocessed image
            cv2.imwrite(output_path, img)
            logger.info(f"✓ Preprocessed image saved: {output_path}")
            
            return True, output_path
            
        except Exception as e:
            logger.error(f"Error in preprocessing pipeline: {e}")
            return False, image_path
    
    def detect_and_crop_document(self, img: np.ndarray) -> Optional[np.ndarray]:
        """
        Detect document contours and apply perspective transform to crop.
        Uses multiple detection methods for better reliability.
        
        Args:
            img: OpenCV image (BGR)
            
        Returns:
            Cropped image or None if detection failed
        """
        try:
            height, width = img.shape[:2]
            image_area = width * height
            
            # Method 1: Enhanced edge detection with adaptive thresholds
            result = self._detect_with_adaptive_canny(img, image_area)
            if result is not None:
                logger.debug("Document detected using adaptive Canny method")
                return result
            
            # Method 2: Morphological operations for better edge detection
            result = self._detect_with_morphology(img, image_area)
            if result is not None:
                logger.debug("Document detected using morphological method")
                return result
            
            # Method 3: Adaptive thresholding for documents with varying lighting
            result = self._detect_with_adaptive_threshold(img, image_area)
            if result is not None:
                logger.debug("Document detected using adaptive threshold method")
                return result
            
            # Method 4: Fallback to simple bounding box if document is mostly rectangular
            result = self._detect_with_bounding_box(img, image_area)
            if result is not None:
                logger.debug("Document detected using bounding box fallback")
                return result
            
            logger.debug("All detection methods failed")
            return None
            
        except Exception as e:
            logger.error(f"Error in document detection: {e}", exc_info=True)
            return None
    
    def _detect_with_adaptive_canny(self, img: np.ndarray, image_area: float) -> Optional[np.ndarray]:
        """Detect document using adaptive Canny edge detection."""
        try:
            # Convert to grayscale
            gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
            
            # Apply bilateral filter to reduce noise while keeping edges
            filtered = cv2.bilateralFilter(gray, 9, 75, 75)
            
            # Calculate adaptive thresholds based on image statistics
            mean_val = np.mean(filtered)
            std_val = np.std(filtered)
            lower = max(50, int(mean_val - std_val))
            upper = min(200, int(mean_val + std_val * 2))
            
            # Edge detection with adaptive thresholds
            edges = cv2.Canny(filtered, lower, upper)
            
            # Dilate edges to connect broken lines
            kernel = np.ones((3, 3), np.uint8)
            edges = cv2.dilate(edges, kernel, iterations=2)
            edges = cv2.erode(edges, kernel, iterations=1)
            
            # Find contours
            contours, _ = cv2.findContours(edges, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
            
            if not contours:
                return None
            
            # Sort contours by area (descending)
            contours = sorted(contours, key=cv2.contourArea, reverse=True)
            
            # Try top 3 largest contours
            for contour in contours[:3]:
                area = cv2.contourArea(contour)
                if area < image_area * self.min_area_ratio:
                    continue
                
                # Approximate with more tolerance
                epsilon = 0.03 * cv2.arcLength(contour, True)
                approx = cv2.approxPolyDP(contour, epsilon, True)
                
                # Accept 4-8 points (more flexible)
                if 4 <= len(approx) <= 8:
                    # If more than 4 points, simplify to 4
                    if len(approx) > 4:
                        # Get bounding rectangle
                        x, y, w, h = cv2.boundingRect(contour)
                        pts = np.array([[x, y], [x+w, y], [x+w, y+h], [x, y+h]], dtype="float32")
                    else:
                        pts = self._order_points(approx.reshape(-1, 2))
                    
                    warped = self._four_point_transform(img, pts)
                    # Validate result (should be reasonable size)
                    if warped.shape[0] > 50 and warped.shape[1] > 50:
                        return warped
            
            return None
        except Exception as e:
            logger.debug(f"Adaptive Canny method failed: {e}")
            return None
    
    def _detect_with_morphology(self, img: np.ndarray, image_area: float) -> Optional[np.ndarray]:
        """Detect document using morphological operations."""
        try:
            gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
            
            # Apply adaptive thresholding
            binary = cv2.adaptiveThreshold(
                gray, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, 
                cv2.THRESH_BINARY_INV, 11, 2
            )
            
            # Morphological operations to close gaps
            kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (5, 5))
            closed = cv2.morphologyEx(binary, cv2.MORPH_CLOSE, kernel, iterations=3)
            
            # Find contours
            contours, _ = cv2.findContours(closed, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
            
            if not contours:
                return None
            
            # Get largest contour
            largest = max(contours, key=cv2.contourArea)
            area = cv2.contourArea(largest)
            
            if area < image_area * self.min_area_ratio:
                return None
            
            # Get bounding rectangle
            x, y, w, h = cv2.boundingRect(largest)
            pts = np.array([[x, y], [x+w, y], [x+w, y+h], [x, y+h]], dtype="float32")
            
            warped = self._four_point_transform(img, pts)
            if warped.shape[0] > 50 and warped.shape[1] > 50:
                return warped
            
            return None
        except Exception as e:
            logger.debug(f"Morphology method failed: {e}")
            return None
    
    def _detect_with_adaptive_threshold(self, img: np.ndarray, image_area: float) -> Optional[np.ndarray]:
        """Detect document using adaptive thresholding."""
        try:
            gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
            
            # Multiple thresholding attempts
            for block_size in [11, 15, 21]:
                binary = cv2.adaptiveThreshold(
                    gray, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C,
                    cv2.THRESH_BINARY_INV, block_size, 2
                )
                
                # Find contours
                contours, _ = cv2.findContours(binary, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
                
                if not contours:
                    continue
                
                # Try largest contour
                largest = max(contours, key=cv2.contourArea)
                area = cv2.contourArea(largest)
                
                if area >= image_area * self.min_area_ratio:
                    x, y, w, h = cv2.boundingRect(largest)
                    pts = np.array([[x, y], [x+w, y], [x+w, y+h], [x, y+h]], dtype="float32")
                    warped = self._four_point_transform(img, pts)
                    if warped.shape[0] > 50 and warped.shape[1] > 50:
                        return warped
            
            return None
        except Exception as e:
            logger.debug(f"Adaptive threshold method failed: {e}")
            return None
    
    def _detect_with_bounding_box(self, img: np.ndarray, image_area: float) -> Optional[np.ndarray]:
        """Fallback: simple bounding box detection."""
        try:
            gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
            
            # Otsu's thresholding
            _, binary = cv2.threshold(gray, 0, 255, cv2.THRESH_BINARY_INV + cv2.THRESH_OTSU)
            
            # Find all non-zero pixels
            coords = np.column_stack(np.where(binary > 0))
            
            if len(coords) == 0:
                return None
            
            # Get bounding box
            rect = cv2.minAreaRect(coords)
            box = cv2.boxPoints(rect)
            box = np.int0(box)
            
            # Check area
            box_area = cv2.contourArea(box)
            if box_area < image_area * self.min_area_ratio:
                return None
            
            # Order points and transform
            pts = self._order_points(box.astype("float32"))
            warped = self._four_point_transform(img, pts)
            
            if warped.shape[0] > 50 and warped.shape[1] > 50:
                return warped
            
            return None
        except Exception as e:
            logger.debug(f"Bounding box method failed: {e}")
            return None
    
    def _order_points(self, pts: np.ndarray) -> np.ndarray:
        """
        Order points in clockwise order: top-left, top-right, bottom-right, bottom-left.
        """
        rect = np.zeros((4, 2), dtype="float32")
        
        # Sum: top-left has smallest sum, bottom-right has largest
        s = pts.sum(axis=1)
        rect[0] = pts[np.argmin(s)]
        rect[2] = pts[np.argmax(s)]
        
        # Diff: top-right has smallest diff, bottom-left has largest
        diff = np.diff(pts, axis=1)
        rect[1] = pts[np.argmin(diff)]
        rect[3] = pts[np.argmax(diff)]
        
        return rect
    
    def _four_point_transform(self, image: np.ndarray, pts: np.ndarray) -> np.ndarray:
        """
        Apply perspective transform to get bird's eye view.
        """
        (tl, tr, br, bl) = pts
        
        # Compute width
        widthA = np.sqrt(((br[0] - bl[0]) ** 2) + ((br[1] - bl[1]) ** 2))
        widthB = np.sqrt(((tr[0] - tl[0]) ** 2) + ((tr[1] - tl[1]) ** 2))
        maxWidth = max(int(widthA), int(widthB))
        
        # Compute height
        heightA = np.sqrt(((tr[0] - br[0]) ** 2) + ((tr[1] - br[1]) ** 2))
        heightB = np.sqrt(((tl[0] - bl[0]) ** 2) + ((tl[1] - bl[1]) ** 2))
        maxHeight = max(int(heightA), int(heightB))
        
        # Destination points
        dst = np.array([
            [0, 0],
            [maxWidth - 1, 0],
            [maxWidth - 1, maxHeight - 1],
            [0, maxHeight - 1]
        ], dtype="float32")
        
        # Compute perspective transform matrix
        M = cv2.getPerspectiveTransform(pts, dst)
        
        # Apply transform
        warped = cv2.warpPerspective(image, M, (maxWidth, maxHeight))
        
        return warped
    
    def deskew_image(self, img: np.ndarray) -> Tuple[Optional[np.ndarray], float]:
        """
        Detect and correct skew angle using Hough line detection.
        
        Args:
            img: OpenCV image (BGR)
            
        Returns:
            Tuple of (deskewed_image, angle_in_degrees)
        """
        try:
            # Convert to grayscale
            gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
            
            # Apply binary threshold
            _, binary = cv2.threshold(gray, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
            
            # Detect edges
            edges = cv2.Canny(binary, 50, 150, apertureSize=3)
            
            # Detect lines using Hough transform
            lines = cv2.HoughLinesP(edges, 1, np.pi/180, 100, minLineLength=100, maxLineGap=10)
            
            if lines is None or len(lines) == 0:
                return None, 0.0
            
            # Calculate angles
            angles = []
            for line in lines:
                x1, y1, x2, y2 = line[0]
                angle = np.degrees(np.arctan2(y2 - y1, x2 - x1))
                angles.append(angle)
            
            # Get median angle
            median_angle = np.median(angles)
            
            # Normalize angle to [-45, 45]
            if median_angle < -45:
                median_angle += 90
            elif median_angle > 45:
                median_angle -= 90
            
            # Rotate image
            if abs(median_angle) > self.deskew_threshold:
                height, width = img.shape[:2]
                center = (width // 2, height // 2)
                M = cv2.getRotationMatrix2D(center, median_angle, 1.0)
                rotated = cv2.warpAffine(img, M, (width, height), flags=cv2.INTER_CUBIC, borderMode=cv2.BORDER_REPLICATE)
                return rotated, median_angle
            
            return img, median_angle
            
        except Exception as e:
            logger.error(f"Error in deskewing: {e}")
            return None, 0.0
    
    def enhance_contrast(self, img: np.ndarray) -> np.ndarray:
        """
        Enhance contrast using CLAHE (Contrast Limited Adaptive Histogram Equalization).
        
        Args:
            img: OpenCV image (BGR)
            
        Returns:
            Contrast-enhanced image
        """
        try:
            # Convert to LAB color space
            lab = cv2.cvtColor(img, cv2.COLOR_BGR2LAB)
            
            # Split channels
            l, a, b = cv2.split(lab)
            
            # Apply CLAHE to L channel
            clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8, 8))
            l = clahe.apply(l)
            
            # Merge channels
            lab = cv2.merge([l, a, b])
            
            # Convert back to BGR
            enhanced = cv2.cvtColor(lab, cv2.COLOR_LAB2BGR)
            
            return enhanced
            
        except Exception as e:
            logger.error(f"Error enhancing contrast: {e}")
            return img
    
    def reduce_noise(self, img: np.ndarray) -> np.ndarray:
        """
        Reduce noise while preserving edges using bilateral filter.
        
        Args:
            img: OpenCV image (BGR)
            
        Returns:
            Denoised image
        """
        try:
            # Bilateral filter: reduces noise while keeping edges sharp
            denoised = cv2.bilateralFilter(img, 9, 75, 75)
            return denoised
            
        except Exception as e:
            logger.error(f"Error reducing noise: {e}")
            return img

