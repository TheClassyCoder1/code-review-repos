import { BadRequestException, Body, Controller, Param, Patch } from '@nestjs/common';
import { CustomerProfileUpdate, ProfileUpdateResult, ProfileValidationError } from './onboarding.dto';
import { ProfileUpdateService } from './profile-update.service';

/** PATCH /portal/profile/:userId — customer self-service profile edits. */
@Controller('portal/profile')
export class ProfileController {
  constructor(private readonly profiles: ProfileUpdateService) {}

  @Patch(':userId')
  update(
    @Param('userId') userId: string,
    @Body() body: Omit<CustomerProfileUpdate, 'userId'>,
  ): ProfileUpdateResult {
    try {
      return this.profiles.update({ ...body, userId: Number(userId) });
    } catch (err) {
      if (err instanceof ProfileValidationError) {
        throw new BadRequestException({ field: err.field, message: err.message });
      }
      throw err;
    }
  }
}
