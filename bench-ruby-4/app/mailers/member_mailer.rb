class MemberMailer < ActionMailer::Base
  default from: "hello@gym.example"

  def welcome
    @member = params[:member]
    mail(to: @member.email, subject: "Welcome to the gym, #{@member.first_name}!")
  end
end
