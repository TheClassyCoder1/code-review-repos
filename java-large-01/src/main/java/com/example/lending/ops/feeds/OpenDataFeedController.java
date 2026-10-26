package com.example.lending.ops.feeds;

import com.example.lending.ops.common.Result.Response;
import com.example.lending.ops.common.Result.SaResult;
import com.example.lending.ops.common.apidoc.ApiOperation;
import com.example.lending.ops.common.apidoc.ApiOperation.*;
import com.example.lending.ops.feeds.DataFeedStreamService.*;
import com.example.lending.ops.security.LoginUserUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** Open API used by partner integration jobs to inspect their data feed streams. */
@RestController
@RequestMapping("/openapi/feeds")
@RequiredArgsConstructor
public class OpenDataFeedController {

    private final DataFeedStreamService streamService;

    @RequestMapping(value = "/stream/get", method = RequestMethod.GET)
    @ApiOperation(value = "Get inlong stream")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "groupId", dataTypeClass = String.class, required = true),
            @ApiImplicitParam(name = "streamId", dataTypeClass = String.class, required = true)
    })
    public Response<InlongStreamInfo> get(@RequestParam String groupId, @RequestParam String streamId) {
        Preconditions.expectNotBlank(groupId, ErrorCodeEnum.INVALID_PARAMETER, "groupId cannot be blank");
        Preconditions.expectNotBlank(streamId, ErrorCodeEnum.INVALID_PARAMETER, "streamId cannot be blank");
        Preconditions.expectNotNull(LoginUserUtils.getLoginUser(), ErrorCodeEnum.LOGIN_USER_EMPTY);
        return Response.success(streamService.get(groupId, streamId));
    }

	@ExceptionHandler
	public SaResult handlerException(Exception e) {
		e.printStackTrace();
		return SaResult.error(e.getMessage());
	}
}
