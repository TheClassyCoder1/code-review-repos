package com.example.lending.loan.servicing.collections.receivables;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.lending.loan.servicing.common.PageRequestParams;

public interface ReceivableRepository extends JpaRepository<Receivable, Long> {

    /** Filters for the receivables of one debtor. */
    public static class ReceivablePageReq extends PageRequestParams {

        private String debtorId;
        private String no;
        private Long contractId;
        private Long planId;

        public String getDebtorId() { return debtorId; }
        public void setDebtorId(String debtorId) { this.debtorId = debtorId; }

        public String getNo() { return no; }
        public void setNo(String no) { this.no = no; }

        public Long getContractId() { return contractId; }
        public void setContractId(Long contractId) { this.contractId = contractId; }

        public Long getPlanId() { return planId; }
        public void setPlanId(Long planId) { this.planId = planId; }
    }

    default Page<Receivable> selectPageByDebtorId(ReceivablePageReq reqVO) {
        Receivable probe = new Receivable();
        probe.setDebtorId(reqVO.getDebtorId());
        probe.setNo(reqVO.getNo());
        probe.setContractId(reqVO.getContractId());
        probe.setPlanId(reqVO.getPlanId());
        return findAll(Example.of(probe), reqVO.toPageable(Sort.by(Sort.Direction.DESC, "id")));
    }
}
