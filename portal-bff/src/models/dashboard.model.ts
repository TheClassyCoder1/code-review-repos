import { Field, Float, Int, ObjectType } from '@nestjs/graphql';

@ObjectType()
export class LoanModel {
  @Field(() => Int) id: number;
  @Field(() => Int) userId: number;
  @Field(() => Float) amount: number;
  @Field() tier: string;
  @Field() status: string;
  @Field({ nullable: true }) applicantSsn?: string;
}

@ObjectType()
export class RiskModel {
  @Field(() => Int) loanId: number;
  @Field(() => Float) score: number;
  @Field() decision: string;
}

@ObjectType()
export class DashboardModel {
  @Field(() => Int) userId: number;
  @Field(() => LoanModel, { nullable: true }) loan: LoanModel | null;
  @Field(() => RiskModel, { nullable: true }) risk: RiskModel | null;
}
