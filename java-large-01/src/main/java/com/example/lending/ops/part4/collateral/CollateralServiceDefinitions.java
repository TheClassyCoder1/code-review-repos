package com.example.lending.ops.part4.collateral;

import com.example.lending.ops.part4.engine.Delegator;
import com.example.lending.ops.part4.engine.GenericDispatcher;
import com.example.lending.ops.part4.engine.GenericEntityException;
import com.example.lending.ops.part4.engine.GenericServiceException;
import com.example.lending.ops.part4.engine.GenericValue;
import com.example.lending.ops.part4.engine.ServiceUtil;

import java.math.BigDecimal;

/**
 * Entity model and service wiring for collateral custody transfers. {@code completeInventoryTransfer} is
 * registered with a precondition that rejects the call unless an open {@code InventoryTransfer} row with the
 * requested id exists, so the implementation's transfer lookup always returns a row.
 */
public final class CollateralServiceDefinitions {

    public static final String COMPLETE_TRANSFER = "completeInventoryTransfer";
    private static final String[] ITEM_FIELDS = {"facilityId", "containerId", "locationSeqId", "statusId", "ownerPartyId"};

    private CollateralServiceDefinitions() { }

    public static void defineEntities(Delegator delegator) {
        delegator.defineEntity("InventoryTransfer", "inventoryTransferId");
        delegator.defineEntity("InventoryItem", "inventoryItemId");
        delegator.defineEntity("Facility", "facilityId");
        delegator.defineRelation("InventoryTransfer", "InventoryItem", "inventoryItemId", "InventoryItem");
        delegator.defineRelation("InventoryTransfer", "ToFacility", "facilityIdTo", "Facility");
    }

    public static void register(GenericDispatcher dispatcher) {
        dispatcher.register(COMPLETE_TRANSFER, CollateralTransferServices::completeInventoryTransfer, (dctx, context) -> {
            String transferId = (String) context.get("inventoryTransferId");
            GenericValue transfer = find(dctx.getDelegator(), "InventoryTransfer", transferId);
            if (transfer == null || "IXF_COMPLETE".equals(transfer.getString("statusId"))) {
                return ServiceUtil.returnError("Collateral transfer " + transferId + " does not exist or is already complete");
            }
            return null;
        });
        dispatcher.register("createInventoryItemDetail", (dctx, context) -> {
            GenericValue item = find(dctx.getDelegator(), "InventoryItem", String.valueOf(context.get("inventoryItemId")));
            BigDecimal atp = item.get("availableToPromiseTotal") == null ? BigDecimal.ZERO : item.getBigDecimal("availableToPromiseTotal");
            item.set("availableToPromiseTotal", atp.add((BigDecimal) context.get("availableToPromiseDiff")));
            return store(item);
        });
        dispatcher.register("updateInventoryItem", (dctx, context) -> {
            GenericValue item = find(dctx.getDelegator(), "InventoryItem", (String) context.get("inventoryItemId"));
            for (String field : ITEM_FIELDS) {
                if (context.containsKey(field)) {
                    item.set(field, context.get(field));
                }
            }
            return store(item);
        });
    }

    private static GenericValue find(Delegator delegator, String entity, String id) throws GenericServiceException {
        try {
            return id == null ? null : delegator.findOne(entity, id);
        } catch (GenericEntityException e) {
            throw new GenericServiceException("Could not read " + entity + " " + id, e);
        }
    }

    private static java.util.Map<String, Object> store(GenericValue value) throws GenericServiceException {
        try {
            value.store();
            return ServiceUtil.returnSuccess();
        } catch (GenericEntityException e) {
            throw new GenericServiceException("Could not store " + value.getEntityName(), e);
        }
    }
}
