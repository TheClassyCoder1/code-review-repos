package com.example.lending.loan.document.export;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
public class LoanExportMessageConverter extends AbstractHttpMessageConverter<LoanExportParams> {

    public enum Compression { NONE, GZIP, ZIP }

    private static final String HEADER_CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding";

    private final LoanExportService metadataExportService;
    private final Compression compression;

    public LoanExportMessageConverter(LoanExportService metadataExportService,
                                      @Value("${document.export.compression:NONE}") Compression compression) {
        super(MediaType.APPLICATION_JSON);
        this.metadataExportService = metadataExportService;
        this.compression = compression;
    }

    @Override
    protected boolean supports(@NonNull Class<?> clazz) {
        return LoanExportParams.class.equals(clazz);
    }

    @Override
    public boolean canRead(@NonNull Class<?> clazz, MediaType mediaType) {
        return false;
    }

    @Override
    @NonNull
    protected LoanExportParams readInternal(@NonNull Class<? extends LoanExportParams> clazz, @NonNull HttpInputMessage inputMessage) {
        throw new HttpMessageNotReadableException("Export parameters cannot be read from a request body", inputMessage);
    }

    @Override
    protected void writeInternal(
        @NonNull LoanExportParams params, HttpOutputMessage outputMessage)
        throws IOException, HttpMessageNotWritableException {
        final String contentDisposition = params.isDownload() ? "attachment; filename=loans.json" : null;
        final boolean attachment = isAttachment(contentDisposition);
        final String extensibleAttachmentFilename =
            getExtensibleAttachmentFilename(contentDisposition, List.of("loans"));

        if (Compression.GZIP == compression) {
            if (!attachment || (extensibleAttachmentFilename != null)) {
                outputMessage
                    .getHeaders()
                    .set(
                        HttpHeaders.CONTENT_DISPOSITION,
                        getContentDispositionHeaderValue(extensibleAttachmentFilename, "gz"));
                outputMessage.getHeaders().set(HEADER_CONTENT_TRANSFER_ENCODING, "binary");
            }

            GZIPOutputStream outputStream = new GZIPOutputStream(outputMessage.getBody());
            metadataExportService.writeLoans(params, outputStream);
            outputStream.close();
        } else if (Compression.ZIP == compression) {
            if (!attachment || (extensibleAttachmentFilename != null)) {
                outputMessage
                    .getHeaders()
                    .set(
                        HttpHeaders.CONTENT_DISPOSITION,
                        getContentDispositionHeaderValue(extensibleAttachmentFilename, "zip"));
                outputMessage.getHeaders().set(HEADER_CONTENT_TRANSFER_ENCODING, "binary");
            }

            ZipOutputStream outputStream = new ZipOutputStream(outputMessage.getBody());
            outputStream.putNextEntry(new ZipEntry("loans.json"));
            metadataExportService.writeLoans(params, outputStream);
            outputStream.close();
        } else {
            if (extensibleAttachmentFilename != null) {
                outputMessage
                    .getHeaders()
                    .set(
                        HttpHeaders.CONTENT_DISPOSITION,
                        getContentDispositionHeaderValue(extensibleAttachmentFilename, null));
            } else if (contentDisposition != null) {
                outputMessage.getHeaders().set(HttpHeaders.CONTENT_DISPOSITION, contentDisposition);
            }

            metadataExportService.writeLoans(params, outputMessage.getBody());
            outputMessage.getBody().close();
        }
    }

    private static boolean isAttachment(String contentDisposition) {
        return contentDisposition != null && contentDisposition.startsWith("attachment");
    }

    private static String getExtensibleAttachmentFilename(String contentDisposition, List<String> names) {
        if (contentDisposition == null) {
            return null;
        }
        for (String name : names) {
            if (contentDisposition.contains("filename=" + name + ".json")) {
                return name + ".json";
            }
        }
        return null;
    }

    private static String getContentDispositionHeaderValue(String filename, String extension) {
        String name = filename == null ? "loans.json" : filename;
        return "attachment; filename=" + (extension == null ? name : name + "." + extension);
    }
}
