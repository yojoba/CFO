"""Tests for image preprocessing service."""
import pytest
import numpy as np
import cv2
from pathlib import Path
import tempfile
import os

from app.services.image_preprocessing_service import ImagePreprocessingService


@pytest.fixture
def preprocessing_service():
    """Create preprocessing service instance."""
    return ImagePreprocessingService()


@pytest.fixture
def create_test_image():
    """Factory to create test images."""
    def _create_image(width=800, height=600, add_border=True, rotate_angle=0):
        """
        Create a test image with a white document on dark background.
        
        Args:
            width: Image width
            height: Image height
            add_border: Add dark border around white document
            rotate_angle: Rotate the document by this angle
        """
        # Create image with dark background
        if add_border:
            img = np.zeros((height, width, 3), dtype=np.uint8)
            # Add white document in center
            doc_margin = 50
            cv2.rectangle(
                img,
                (doc_margin, doc_margin),
                (width - doc_margin, height - doc_margin),
                (255, 255, 255),
                -1
            )
        else:
            # Just white image
            img = np.ones((height, width, 3), dtype=np.uint8) * 255
        
        # Add some text-like patterns
        for i in range(10):
            y = 100 + i * 40
            cv2.line(img, (100, y), (width - 100, y), (0, 0, 0), 2)
        
        # Rotate if requested
        if rotate_angle != 0:
            center = (width // 2, height // 2)
            M = cv2.getRotationMatrix2D(center, rotate_angle, 1.0)
            img = cv2.warpAffine(img, M, (width, height))
        
        return img
    
    return _create_image


def test_preprocess_pipeline_success(preprocessing_service, create_test_image, tmp_path):
    """Test complete preprocessing pipeline."""
    # Create test image
    test_img = create_test_image(add_border=True, rotate_angle=5)
    
    # Save to temp file
    input_path = tmp_path / "test_input.jpg"
    output_path = tmp_path / "test_output.jpg"
    cv2.imwrite(str(input_path), test_img)
    
    # Run preprocessing
    success, result_path = preprocessing_service.preprocess_for_ocr(
        str(input_path),
        str(output_path)
    )
    
    # Verify
    assert success
    assert result_path == str(output_path)
    assert os.path.exists(output_path)
    
    # Load result and check it's valid
    result_img = cv2.imread(str(output_path))
    assert result_img is not None
    assert result_img.shape[0] > 0
    assert result_img.shape[1] > 0


def test_detect_and_crop_document(preprocessing_service, create_test_image):
    """Test document detection and cropping."""
    # Create image with border
    test_img = create_test_image(width=1000, height=800, add_border=True)
    
    # Detect and crop
    cropped = preprocessing_service.detect_and_crop_document(test_img)
    
    # Should return cropped image
    assert cropped is not None
    # Cropped image should be smaller than original
    assert cropped.shape[0] <= test_img.shape[0]
    assert cropped.shape[1] <= test_img.shape[1]


def test_detect_and_crop_no_document(preprocessing_service):
    """Test detection fails gracefully when no document detected."""
    # Create image with no clear document (just noise)
    test_img = np.random.randint(0, 255, (600, 800, 3), dtype=np.uint8)
    
    # Should return None or original
    result = preprocessing_service.detect_and_crop_document(test_img)
    # Either None or some result, but shouldn't crash
    assert result is None or isinstance(result, np.ndarray)


def test_deskew_image(preprocessing_service, create_test_image):
    """Test image deskewing."""
    # Create rotated image
    test_img = create_test_image(rotate_angle=10)
    
    # Deskew
    deskewed, angle = preprocessing_service.deskew_image(test_img)
    
    # Should detect rotation
    assert deskewed is not None or angle != 0
    if deskewed is not None:
        assert deskewed.shape == test_img.shape


def test_deskew_straight_image(preprocessing_service, create_test_image):
    """Test deskewing on already straight image."""
    # Create straight image
    test_img = create_test_image(rotate_angle=0)
    
    # Deskew
    deskewed, angle = preprocessing_service.deskew_image(test_img)
    
    # Angle should be small
    assert abs(angle) < 5.0  # Allow small detection error


def test_enhance_contrast(preprocessing_service, create_test_image):
    """Test contrast enhancement."""
    # Create low contrast image
    test_img = create_test_image()
    # Reduce contrast
    test_img = (test_img * 0.5 + 128 * 0.5).astype(np.uint8)
    
    # Enhance
    enhanced = preprocessing_service.enhance_contrast(test_img)
    
    # Should return valid image
    assert enhanced is not None
    assert enhanced.shape == test_img.shape
    assert enhanced.dtype == np.uint8


def test_reduce_noise(preprocessing_service, create_test_image):
    """Test noise reduction."""
    # Create image
    test_img = create_test_image()
    # Add noise
    noise = np.random.randint(-20, 20, test_img.shape, dtype=np.int16)
    noisy_img = np.clip(test_img.astype(np.int16) + noise, 0, 255).astype(np.uint8)
    
    # Reduce noise
    denoised = preprocessing_service.reduce_noise(noisy_img)
    
    # Should return valid image
    assert denoised is not None
    assert denoised.shape == noisy_img.shape
    assert denoised.dtype == np.uint8


def test_preprocessing_with_invalid_path(preprocessing_service, tmp_path):
    """Test preprocessing handles invalid file path gracefully."""
    input_path = tmp_path / "nonexistent.jpg"
    output_path = tmp_path / "output.jpg"
    
    success, result_path = preprocessing_service.preprocess_for_ocr(
        str(input_path),
        str(output_path)
    )
    
    # Should fail gracefully
    assert not success
    assert result_path == str(input_path)  # Returns original path on failure


def test_order_points(preprocessing_service):
    """Test point ordering for perspective transform."""
    # Create unordered quadrilateral points
    pts = np.array([
        [100, 200],  # bottom-left
        [100, 100],  # top-left
        [200, 100],  # top-right
        [200, 200],  # bottom-right
    ], dtype=np.float32)
    
    ordered = preprocessing_service._order_points(pts)
    
    # Check ordering: tl, tr, br, bl
    assert ordered[0][0] < ordered[1][0]  # tl.x < tr.x
    assert ordered[0][1] < ordered[3][1]  # tl.y < bl.y
    assert ordered[2][0] > ordered[3][0]  # br.x > bl.x
    assert ordered[1][1] < ordered[2][1]  # tr.y < br.y


def test_preprocessing_performance(preprocessing_service, create_test_image, tmp_path):
    """Test that preprocessing completes in reasonable time."""
    import time
    
    # Create test image
    test_img = create_test_image(width=2000, height=1500)
    input_path = tmp_path / "test_perf.jpg"
    output_path = tmp_path / "test_perf_out.jpg"
    cv2.imwrite(str(input_path), test_img)
    
    # Measure time
    start = time.time()
    success, _ = preprocessing_service.preprocess_for_ocr(
        str(input_path),
        str(output_path)
    )
    duration = time.time() - start
    
    # Should complete in under 5 seconds for 2000x1500 image
    assert success
    assert duration < 5.0, f"Preprocessing took {duration:.2f}s, expected < 5s"


if __name__ == "__main__":
    pytest.main([__file__, "-v"])

