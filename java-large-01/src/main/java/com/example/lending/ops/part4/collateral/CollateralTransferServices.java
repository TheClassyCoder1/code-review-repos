package com.example.lending.ops.part4.collateral;

import com.example.lending.ops.part4.engine.Delegator;
import com.example.lending.ops.part4.engine.DispatchContext;
import com.example.lending.ops.part4.engine.EntityQuery;
import com.example.lending.ops.part4.engine.GenericEntityException;
import com.example.lending.ops.part4.engine.GenericServiceException;
import com.example.lending.ops.part4.engine.GenericValue;
import com.example.lending.ops.part4.engine.ServiceUtil;
import com.example.lending.ops.part4.engine.UtilDateTime;
import com.example.lending.ops.part4.engine.UtilMisc;
import com.example.lending.ops.part4.engine.UtilProperties;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Locale;
import java.util.Map;

/**
 * Moves pledged collateral (title deeds, vehicle papers, gold) between custody facilities. A transfer is opened
 * when the item leaves the source vault and completed here once the destination vault books it in.
 */
public final class CollateralTransferServices {

    private static final String RESOURCE = "i18n/collateral";

    private CollateralTransferServices() {
    }

    public static Map<String, Object> completeInventoryTransfer(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String inventoryTransferId = (String) context.get("inventoryTransferId");
        Timestamp receiveDate = (Timestamp) context.get("receiveDate");
        GenericValue inventoryTransfer = null;
        GenericValue inventoryItem = null;
        GenericValue destinationFacility = null;
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        try {
            inventoryTransfer = EntityQuery.use(delegator).from("InventoryTransfer").where("inventoryTransferId", inventoryTransferId).queryOne();
            inventoryItem = inventoryTransfer.getRelatedOne("InventoryItem", false);
            destinationFacility = inventoryTransfer.getRelatedOne("ToFacility", false);
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "ProductInventoryItemLookupProblem",
                    UtilMisc.toMap("errorString", e.getMessage()), locale));
        }

        if (inventoryItem == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "ProductInventoryItemLookupProblem",
                    UtilMisc.toMap("errorString", ""), locale));
        }

        String inventoryType = inventoryItem.getString("inventoryItemTypeId");

        // set the fields on the transfer record
        if (inventoryTransfer.get("receiveDate") == null) {
            if (receiveDate != null) {
                inventoryTransfer.set("receiveDate", receiveDate);
            } else {
                inventoryTransfer.set("receiveDate", UtilDateTime.nowTimestamp());
            }
        }

        if ("NON_SERIAL_INV_ITEM".equals(inventoryType)) {
            // add an adjusting InventoryItemDetail so set ATP back to QOH: ATP = ATP + (QOH - ATP), diff = QOH - ATP
            BigDecimal atp = inventoryItem.get("availableToPromiseTotal") == null ? BigDecimal.ZERO
                    : inventoryItem.getBigDecimal("availableToPromiseTotal");
            BigDecimal qoh = inventoryItem.get("quantityOnHandTotal") == null ? BigDecimal.ZERO : inventoryItem.getBigDecimal("quantityOnHandTotal");
            Map<String, Object> createDetailMap = UtilMisc.toMap("availableToPromiseDiff", qoh.subtract(atp),
                    "inventoryItemId", inventoryItem.get("inventoryItemId"), "userLogin", userLogin);
            try {
                Map<String, Object> result = dctx.getDispatcher().runSync("createInventoryItemDetail", createDetailMap);
                if (ServiceUtil.isError(result)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                            "ProductInventoryItemDetailCreateProblem",
                            UtilMisc.toMap("errorString", ""), locale), null, null, result);
                }
            } catch (GenericServiceException e1) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        "ProductInventoryItemDetailCreateProblem",
                        UtilMisc.toMap("errorString", e1.getMessage()), locale));
            }
            try {
                inventoryItem.refresh();
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        "ProductInventoryItemRefreshProblem",
                        UtilMisc.toMap("errorString", e.getMessage()), locale));
            }
        }

        // set the fields on the item
        Map<String, Object> updateInventoryItemMap = UtilMisc.toMap("inventoryItemId", inventoryItem.getString("inventoryItemId"),
                                                    "facilityId", inventoryTransfer.get("facilityIdTo"),
                                                    "containerId", inventoryTransfer.get("containerIdTo"),
                                                    "locationSeqId", inventoryTransfer.get("locationSeqIdTo"),
                                                    "userLogin", userLogin);

        // for serialized items, automatically make them available
        if ("SERIALIZED_INV_ITEM".equals(inventoryType)) {
            updateInventoryItemMap.put("statusId", "INV_AVAILABLE");
        }

        // if the destination facility's owner is different
        // from the inventory item's ownwer,
        // the inventory item is assigned to the new owner.
        if (destinationFacility != null && destinationFacility.get("ownerPartyId") != null) {
            String fromPartyId = inventoryItem.getString("ownerPartyId");
            String toPartyId = destinationFacility.getString("ownerPartyId");
            if (fromPartyId == null || !fromPartyId.equals(toPartyId)) {
                updateInventoryItemMap.put("ownerPartyId", toPartyId);
            }
        }
        try {
            Map<String, Object> result = dctx.getDispatcher().runSync("updateInventoryItem", updateInventoryItemMap);
            if (ServiceUtil.isError(result)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        "ProductInventoryItemStoreProblem",
                        UtilMisc.toMap("errorString", ""), locale), null, null, result);
            }
        } catch (GenericServiceException exc) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "ProductInventoryItemStoreProblem",
                    UtilMisc.toMap("errorString", exc.getMessage()), locale));
        }

        // set the inventory transfer record to complete
        inventoryTransfer.set("statusId", "IXF_COMPLETE");

        // store the entities
        try {
            inventoryTransfer.store();
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "ProductInventoryItemStoreProblem",
                    UtilMisc.toMap("errorString", e.getMessage()), locale));
        }

        return ServiceUtil.returnSuccess();
    }
}
