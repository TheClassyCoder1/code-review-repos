from decimal import Decimal
from typing import Optional

from pydantic import BaseModel, ConfigDict, Field


class JobUpdate(BaseModel):
    model_config = ConfigDict(extra="forbid")

    title: Optional[str] = Field(default=None, max_length=120)
    schedule: Optional[str] = Field(default=None, pattern=r"^(daily|weekly|monthly)$")
    recipients: Optional[list[str]] = Field(default=None, max_length=20)


class InvoiceLine(BaseModel):
    description: str = Field(max_length=200)
    unit_price: Decimal = Field(max_digits=12, decimal_places=4)
    quantity: int = Field(ge=0)


class ReportRequest(BaseModel):
    template: str = Field(pattern=r"^[a-z0-9_]{3,40}$")
    period: str = Field(pattern=r"^\d{4}-(0[1-9]|1[0-2])$")
    lines: list[InvoiceLine] = Field(default_factory=list)
