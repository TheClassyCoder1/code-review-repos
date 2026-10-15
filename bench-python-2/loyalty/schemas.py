from decimal import Decimal

from pydantic import BaseModel, Field


class EnrollRequest(BaseModel):
    email: str = Field(min_length=3, max_length=255, pattern=r"^[^@\s]+@[^@\s]+$")
    full_name: str = Field(min_length=1, max_length=120)
    referred_by_id: int | None = None


class ProfileUpdate(BaseModel):
    full_name: str = Field(min_length=1, max_length=120)


class AdjustmentRequest(BaseModel):
    points: int = Field(ge=-100_000, le=100_000)
    reason: str = Field(min_length=3, max_length=64)


class TransferRequest(BaseModel):
    to_member_id: int
    points: int = Field(gt=0, le=1_000_000)


class EarnRequest(BaseModel):
    member_id: int
    amount: Decimal = Field(gt=0, max_digits=12, decimal_places=2)
    reference: str = Field(min_length=1, max_length=64)


class TransactionBatch(BaseModel):
    transactions: list[EarnRequest] = Field(max_length=500)
    checksum: str = Field(min_length=64, max_length=64)


class RuleCreate(BaseModel):
    partner_id: int
    name: str = Field(min_length=1, max_length=80)
    multiplier: Decimal = Field(gt=0, le=10, max_digits=6, decimal_places=3)
    min_spend: Decimal = Field(default=Decimal("0"), ge=0, max_digits=12, decimal_places=2)


class TierPreviewRequest(BaseModel):
    level: int = Field(ge=0, le=3)
    lifetime_points: int = Field(ge=0)


class RedeemRequest(BaseModel):
    reward_id: int
