package com.example.lending.loan.servicing.collections.waivers;

import com.example.lending.loan.servicing.common.ServicingException;
import com.example.lending.loan.servicing.security.portal.CurrentBorrower;
import com.example.lending.loan.servicing.security.portal.PortalPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Date;
import java.util.List;

/** Borrower self-service claims against fee waiver campaigns. */
@Service
public class FeeWaiverClaimService {

    private static final char[] CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    private final CurrentBorrower currentBorrower;
    private final FeeWaiverCampaignRepository campaignRepository;
    private final FeeWaiverClaimRepository claimRepository;
    private final SecureRandom random = new SecureRandom();

    public FeeWaiverClaimService(CurrentBorrower currentBorrower, FeeWaiverCampaignRepository campaignRepository,
                                 FeeWaiverClaimRepository claimRepository) {
        this.currentBorrower = currentBorrower;
        this.campaignRepository = campaignRepository;
        this.claimRepository = claimRepository;
    }

    @Transactional
    public void add(Long campaignId) {
        PortalPrincipal currentMember = currentBorrower.get();
        FeeWaiverCampaign campaign = campaignRepository.findById(campaignId).orElse(null);
        if (campaign == null) {
            throw ServicingException.notFound("Waiver campaign does not exist");
        }
        if (campaign.getCount() <= 0) {
            throw ServicingException.conflict("All waivers of this campaign have been claimed");
        }
        Date now = new Date();
        if (now.before(campaign.getEnableTime())) {
            throw ServicingException.conflict("Waiver campaign is not open yet");
        }
        long count = claimRepository.countByCampaignIdAndBorrowerId(campaignId, currentMember.borrowerId());
        if (count >= campaign.getPerLimit()) {
            throw ServicingException.conflict("You have already claimed this waiver");
        }
        FeeWaiverClaim claim = new FeeWaiverClaim();
        claim.setCampaignId(campaignId);
        claim.setClaimCode(generateClaimCode(currentMember.borrowerId()));
        claim.setCreateTime(now);
        claim.setBorrowerId(currentMember.borrowerId());
        claim.setGetType(FeeWaiverClaim.GET_TYPE_SELF_SERVICE);
        claim.setUseStatus(FeeWaiverClaim.USE_STATUS_UNUSED);
        claimRepository.save(claim);
        campaign.setCount(campaign.getCount() - 1);
        campaign.setReceiveCount(campaign.getReceiveCount() == null ? 1 : campaign.getReceiveCount() + 1);
        campaignRepository.save(campaign);
    }

    @Transactional(readOnly = true)
    public List<FeeWaiverClaim> listMine() {
        return claimRepository.findByBorrowerIdOrderByCreateTimeDesc(currentBorrower.borrowerId());
    }

    private String generateClaimCode(Long borrowerId) {
        StringBuilder code = new StringBuilder("FW").append(borrowerId % 10000).append('-');
        for (int i = 0; i < 10; i++) {
            code.append(CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)]);
        }
        return code.toString();
    }
}
