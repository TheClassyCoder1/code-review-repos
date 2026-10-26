/**
 * Dealer and broker partners that hand borrowers over to the portal and receive
 * them back once an application step is complete. The return URLs are part of
 * each partner's signed onboarding agreement and are only changed by a release.
 */
export interface Partner {
  id: string;
  returnUrl: string;
  /** Products the partner is allowed to originate. */
  products: readonly string[];
}

const PARTNERS: ReadonlyMap<string, Partner> = new Map(
  [
    { id: 'dealer-northgate', returnUrl: 'https://portal.northgate-motors.test/finance/return', products: ['auto'] },
    { id: 'broker-harbor', returnUrl: 'https://apply.harbor-brokers.test/lender-callback', products: ['mortgage', 'heloc'] },
    { id: 'retail-solaris', returnUrl: 'https://checkout.solaris-energy.test/financing/complete', products: ['personal'] },
  ].map((p): [string, Partner] => [p.id, p]),
);

export function findPartner(partnerId: string): Partner | undefined {
  return PARTNERS.get(partnerId);
}
