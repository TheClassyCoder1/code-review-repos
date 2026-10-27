package com.example.lending.loan.notify.template;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/ops/notification-templates")
public class NotificationTemplateController {

    private static final Pattern DEFINITION_NAME = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");

    private final NotificationTemplateService templateService;
    private final TemplateDefinitionStore definitionStore;
    private final TemplateMessageSource messageSource;

    public NotificationTemplateController(NotificationTemplateService templateService,
                                          TemplateDefinitionStore definitionStore,
                                          TemplateMessageSource messageSource) {
        this.templateService = templateService;
        this.definitionStore = definitionStore;
        this.messageSource = messageSource;
    }

    @PostMapping
    public Map<String, Long> create(@RequestBody NotificationTemplateSaveRequest request) {
        return Map.of("id", templateService.createTemplate(request));
    }

    @PostMapping("/messages")
    public ResponseEntity<Void> overrideMessage(@RequestBody TemplateMessageSource.PresentationMessage message) {
        messageSource.addPresentation(message);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/definitions/{name}")
    public ResponseEntity<Void> deleteDefinition(@PathVariable String name) {
        if (!DEFINITION_NAME.matcher(name).matches()) {
            return ResponseEntity.badRequest().build();
        }
        definitionStore.delete(name);
        return ResponseEntity.noContent().build();
    }
}
