package com.subscribers.gudogging.domain.subscription.domain.screenshot;

import com.subscribers.gudogging.domain.subscription.application.exception.InvalidScreenshotCountException;
import com.subscribers.gudogging.domain.subscription.application.exception.ScreenshotTooLargeException;
import com.subscribers.gudogging.domain.subscription.application.exception.UnsupportedScreenshotFormatException;
import java.util.List;

/**
 * 분석할 스크린샷 1장 (A-07). 메모리에서만 다루고 저장하지 않는다. contentType은 파일 시그니처로 정한다 — 확장자·요청 Content-Type은 믿지 않는다.
 */
public record ScreenshotImage(String filename, String contentType, byte[] content) {

    public static final int MAX_COUNT = 5;
    public static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;

    private static final String PNG = "image/png";
    private static final String JPEG = "image/jpeg";
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    /**
     * 10MB 이하인 PNG·JPEG만 만든다.
     */
    public static ScreenshotImage of(String filename, byte[] content) {
        if (content.length > MAX_SIZE_BYTES) {
            throw new ScreenshotTooLargeException();
        }
        return new ScreenshotImage(filename, contentTypeOf(content), content);
    }

    /**
     * 한 번에 1~5장만 분석할 수 있다.
     */
    public static void validateCount(List<?> images) {
        if (images == null || images.isEmpty() || images.size() > MAX_COUNT) {
            throw new InvalidScreenshotCountException();
        }
    }

    private static String contentTypeOf(byte[] content) {
        if (startsWith(content, PNG_SIGNATURE)) {
            return PNG;
        }
        if (startsWith(content, JPEG_SIGNATURE)) {
            return JPEG;
        }
        throw new UnsupportedScreenshotFormatException();
    }

    private static boolean startsWith(byte[] content, byte[] signature) {
        if (content.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (content[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }
}
