package ch.framedev.lagersystem.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for generating QR code images using ZXing.
 */
public class QRCodeGenerator {

    /**
     * Generates a QR code image for the given data and saves it as a PNG file.
     *
     * @param data      The data to encode in the QR code
     * @param width     The width of the QR code image
     * @param height    The height of the QR code image
     * @param fileName  The name of the output PNG file
     * @return The generated QR code image file
     * @throws WriterException If an error occurs during QR code generation
     * @throws IOException     If an error occurs while writing the file
     */
    public static File generateQRCodeImage(String data, int width, int height, String fileName) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, width, height, hints);

        Path path = FileSystems.getDefault().getPath(fileName);
        MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);
        return path.toFile();
    }

    /**
     * Generates a QR code image as a BufferedImage (for in-memory use).
     *
     * @param data   The data to encode
     * @param width  The width of the image
     * @param height The height of the image
     * @return BufferedImage containing the QR code
     * @throws WriterException If an error occurs during QR code generation
     */
    public static BufferedImage generateQRCodeBufferedImage(String data, int width, int height) throws WriterException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, width, height, hints);
        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }
}
