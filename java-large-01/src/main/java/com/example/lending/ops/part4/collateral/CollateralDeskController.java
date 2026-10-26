package com.example.lending.ops.part4.collateral;

import com.example.lending.ops.common.Result;
import com.example.lending.ops.part4.engine.GenericDispatcher;
import com.example.lending.ops.part4.engine.GenericServiceException;
import com.example.lending.ops.part4.engine.ServiceUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.Map;

/** Collateral custody desk: book in collateral that arrived at the destination vault. */
@RestController
@RequestMapping("/console/collateral")
@RequiredArgsConstructor
public class CollateralDeskController {

    private final GenericDispatcher backOfficeDispatcher;

    @PostMapping("/transfers/{transferId}/complete")
    public Result<Map<String, Object>> completeTransfer(@PathVariable String transferId, Locale locale)
            throws GenericServiceException {
        Map<String, Object> result = backOfficeDispatcher.runSync(CollateralServiceDefinitions.COMPLETE_TRANSFER,
                Map.of("inventoryTransferId", transferId, "locale", locale));
        return ServiceUtil.isError(result) ? Result.error(String.valueOf(result.get(ServiceUtil.ERROR_MESSAGE))) : Result.OK(result);
    }
}
