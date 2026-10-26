using Lending.Part2;
using Lending.Part2.Branches;
using Lending.Part2.Links;
using Microsoft.AspNetCore.Authentication.Cookies;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddControllers();
builder.Services
    .AddAuthentication(CookieAuthenticationDefaults.AuthenticationScheme)
    .AddCookie(options =>
    {
        options.LoginPath = "/servicing/account/login";
        options.Cookie.Name = "servicing.session";
        options.Cookie.HttpOnly = true;
        options.Cookie.SecurePolicy = CookieSecurePolicy.Always;
        options.SlidingExpiration = true;
        options.ExpireTimeSpan = TimeSpan.FromMinutes(30);
    });
builder.Services.AddAuthorization();
builder.Services.AddServicingPart2(builder.Configuration, builder.Environment);

var app = builder.Build();

app.UseAuthentication();
app.UseAuthorization();
app.UseWhen(
    context => context.Request.Path.StartsWithSegments("/servicing/branch"),
    branch => branch.UseMiddleware<BranchContextMiddleware>());

app.MapControllers();
app.MapPaymentLinks();

app.Run();
