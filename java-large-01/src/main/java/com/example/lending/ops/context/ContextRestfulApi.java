package com.example.lending.ops.context;

import com.example.lending.ops.common.Result.Message;
import com.example.lending.ops.common.StringUtils;
import com.example.lending.ops.common.apidoc.ApiOperation;
import com.example.lending.ops.common.apidoc.ApiOperation.*;
import com.example.lending.ops.context.ContextService.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.text.MessageFormat;

import static com.example.lending.ops.context.ContextService.CsServerErrorCodeSummary.PARAMS_CANNOT_EMPTY;

@RestController
@RequestMapping(path = "/contextservice")
@RequiredArgsConstructor
public class ContextRestfulApi {

  private final ContextService contextService;
  private final ObjectMapper objectMapper;

  @ApiOperation(value = "setValue", notes = "set value", response = Message.class)
  @ApiImplicitParams({
    @ApiImplicitParam(name = "contextKeyValue", required = true, dataType = "String"),
    @ApiImplicitParam(name = "contextID", required = true, dataType = "String")
  })
  @ApiOperationSupport(ignoreParameters = {"jsonNode"})
  @RequestMapping(path = "setValue", method = RequestMethod.POST)
  public Message setValue(HttpServletRequest req, @RequestBody JsonNode jsonNode)
      throws InterruptedException, CSErrorException, IOException, ClassNotFoundException {
    ContextID contextID = getContextIDFromJsonNode(jsonNode);
    if (StringUtils.isEmpty(contextID.getContextId())) {
      throw new CSErrorException(
          PARAMS_CANNOT_EMPTY.getErrorCode(),
          MessageFormat.format(PARAMS_CANNOT_EMPTY.getErrorDesc(), "ContextID"));
    }
    ModuleUserUtils.getOperationUser(req, "setValue,contextID:" + contextID.getContextId());
    ContextKeyValue contextKeyValue = getContextKeyValueFromJsonNode(jsonNode);
    contextService.setValue(contextID, contextKeyValue);
    return generateMessage(null, "");
  }

  @ApiOperation(
      value = "removeAllValueByKeyPrefixAndContextType",
      notes = "remove all value by key prefix and context type",
      response = Message.class)
  @ApiImplicitParams({
    @ApiImplicitParam(name = "contextKeyType", required = true, dataType = "String"),
    @ApiImplicitParam(name = "keyPrefix", required = true, dataType = "String")
  })
  @ApiOperationSupport(ignoreParameters = {"jsonNode"})
  @RequestMapping(path = "removeAllValueByKeyPrefixAndContextType", method = RequestMethod.POST)
  public Message removeAllValueByKeyPrefixAndContextType(
      HttpServletRequest req, @RequestBody JsonNode jsonNode)
      throws InterruptedException, CSErrorException, IOException, ClassNotFoundException {
    ContextID contextID = getContextIDFromJsonNode(jsonNode);
    if (StringUtils.isEmpty(contextID.getContextId())) {
      throw new CSErrorException(
          PARAMS_CANNOT_EMPTY.getErrorCode(),
          MessageFormat.format(PARAMS_CANNOT_EMPTY.getErrorDesc(), "ContextID"));
    }
    ModuleUserUtils.getOperationUser(
        req, "removeAllValueByKeyPrefixAndContextType,contextID:" + contextID.getContextId());
    String contextType = jsonNode.get(ContextHTTPConstant.CONTEXT_KEY_TYPE_STR).textValue();
    String keyPrefix = jsonNode.get(ContextHTTPConstant.CONTEXT_KEY_PREFIX_STR).textValue();
    contextService.removeAllValueByKeyPrefixAndContextType(
        contextID, ContextType.valueOf(contextType), keyPrefix);
    return generateMessage(null, "");
  }

  private ContextID getContextIDFromJsonNode(JsonNode jsonNode) {
    return new ContextID(jsonNode.path(ContextHTTPConstant.CONTEXT_ID_STR).asText(null));
  }

  private ContextKeyValue getContextKeyValueFromJsonNode(JsonNode jsonNode) throws IOException {
    return objectMapper.treeToValue(jsonNode.get(ContextHTTPConstant.CONTEXT_KEY_VALUE_STR), ContextKeyValue.class);
  }

  private Message<Object> generateMessage(Object data, String key) {
    return new Message<>(0, key, data);
  }
}
