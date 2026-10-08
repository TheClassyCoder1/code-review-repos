package com.example.lending.loan.partner.docs;

import com.example.lending.loan.partner.account.PartnerSession;
import com.example.lending.loan.support.web.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class PartnerDocumentUploadController {

    private static final Logger _logger = LoggerFactory.getLogger(PartnerDocumentUploadController.class);

    private final PartnerDocumentRepository fileUploadService;

    public PartnerDocumentUploadController(PartnerDocumentRepository fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    @PostMapping({"/partner/documents/upload"})
    public ApiResponse<Object> upload(HttpServletRequest request,
                                      @ModelAttribute PartnerDocument fileUpload) {
        _logger.debug("FileUpload");
        fileUpload.setId(fileUpload.generateId());
        fileUpload.setContentType(fileUpload.getUploadFile().getContentType());
        fileUpload.setFileName(fileUpload.getUploadFile().getOriginalFilename());
        fileUpload.setContentSize(fileUpload.getUploadFile().getSize());
        fileUpload.setCreatedBy(PartnerSession.userId(request));
        /*
         * upload UploadFile MultipartFile to Uploaded Bytes
         */
        if(null!=fileUpload.getUploadFile()&&!fileUpload.getUploadFile().isEmpty()){
            try {
                fileUpload.setUploaded(fileUpload.getUploadFile().getBytes());
                fileUploadService.save(fileUpload);
                _logger.trace("FileUpload SUCCESS");
            } catch (IOException e) {
                _logger.error("FileUpload IOException",e);
            }
        }
        return ApiResponse.ofSuccess(fileUpload.getId());
    }
}
