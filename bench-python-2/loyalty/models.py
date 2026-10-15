from datetime import date, datetime
from decimal import Decimal

from sqlalchemy import (
    Boolean,
    Date,
    DateTime,
    ForeignKey,
    Integer,
    Numeric,
    String,
    UniqueConstraint,
    func,
)
from sqlalchemy.orm import Mapped, mapped_column

from .db import Base


class Member(Base):
    __tablename__ = "members"
    __table_args__ = (UniqueConstraint("email", name="uq_members_email"),)

    id: Mapped[int] = mapped_column(primary_key=True)
    email: Mapped[str] = mapped_column(String(255))
    full_name: Mapped[str] = mapped_column(String(120))
    tier: Mapped[str] = mapped_column(String(20), default="member")
    status: Mapped[str] = mapped_column(String(20), default="active")
    points_balance: Mapped[int] = mapped_column(Integer, default=0)
    lifetime_points: Mapped[int] = mapped_column(Integer, default=0)
    referred_by_id: Mapped[int | None] = mapped_column(ForeignKey("members.id"), nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())


class Partner(Base):
    __tablename__ = "partners"

    id: Mapped[int] = mapped_column(primary_key=True)
    code: Mapped[str] = mapped_column(String(32), unique=True)
    brand_name: Mapped[str] = mapped_column(String(80))
    webhook_secret: Mapped[str] = mapped_column(String(128))
    settlement_rate: Mapped[Decimal] = mapped_column(Numeric(10, 4))
    active: Mapped[bool] = mapped_column(Boolean, default=True)


class EarnRule(Base):
    __tablename__ = "earn_rules"

    id: Mapped[int] = mapped_column(primary_key=True)
    partner_id: Mapped[int] = mapped_column(ForeignKey("partners.id"))
    name: Mapped[str] = mapped_column(String(80))
    multiplier: Mapped[Decimal] = mapped_column(Numeric(6, 3))
    min_spend: Mapped[Decimal] = mapped_column(Numeric(12, 2), default=Decimal("0"))
    active: Mapped[bool] = mapped_column(Boolean, default=True)


class LedgerEntry(Base):
    __tablename__ = "ledger_entries"

    id: Mapped[int] = mapped_column(primary_key=True)
    member_id: Mapped[int] = mapped_column(ForeignKey("members.id"), index=True)
    partner_id: Mapped[int | None] = mapped_column(ForeignKey("partners.id"), nullable=True)
    kind: Mapped[str] = mapped_column(String(20))
    points: Mapped[int] = mapped_column(Integer)
    reference: Mapped[str] = mapped_column(String(64))
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())


class Reward(Base):
    __tablename__ = "rewards"

    id: Mapped[int] = mapped_column(primary_key=True)
    partner_id: Mapped[int] = mapped_column(ForeignKey("partners.id"))
    name: Mapped[str] = mapped_column(String(120))
    cost_points: Mapped[int] = mapped_column(Integer)
    active: Mapped[bool] = mapped_column(Boolean, default=True)


class Redemption(Base):
    __tablename__ = "redemptions"

    id: Mapped[int] = mapped_column(primary_key=True)
    member_id: Mapped[int] = mapped_column(ForeignKey("members.id"), index=True)
    reward_id: Mapped[int] = mapped_column(ForeignKey("rewards.id"))
    points: Mapped[int] = mapped_column(Integer)
    status: Mapped[str] = mapped_column(String(20), default="pending")
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())


class PartnerNotification(Base):
    __tablename__ = "partner_notifications"

    id: Mapped[int] = mapped_column(primary_key=True)
    partner_id: Mapped[int] = mapped_column(ForeignKey("partners.id"))
    redemption_id: Mapped[int] = mapped_column(ForeignKey("redemptions.id"))
    status: Mapped[str] = mapped_column(String(20), default="pending")
    attempts: Mapped[int] = mapped_column(Integer, default=0)


class Statement(Base):
    __tablename__ = "statements"

    id: Mapped[int] = mapped_column(primary_key=True)
    member_id: Mapped[int] = mapped_column(ForeignKey("members.id"), index=True)
    period_start: Mapped[date] = mapped_column(Date)
    period_end: Mapped[date] = mapped_column(Date)
    opening_balance: Mapped[int] = mapped_column(Integer)
    closing_balance: Mapped[int] = mapped_column(Integer)
    file_name: Mapped[str | None] = mapped_column(String(64), nullable=True)
