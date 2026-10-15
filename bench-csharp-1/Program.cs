using Microsoft.AspNetCore.Authentication.Cookies;
using Newtonsoft.Json.Serialization;
using Payroll.Data;
using Payroll.Security;
using Payroll.Services;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddControllers(options => options.Filters.Add<LocalReturnUrlFilter>());
builder.Services
    .AddAuthentication(CookieAuthenticationDefaults.AuthenticationScheme)
    .AddCookie(options =>
    {
        options.Cookie.HttpOnly = true;
        options.Cookie.SecurePolicy = CookieSecurePolicy.Always;
        options.LoginPath = "/auth/login";
    });
builder.Services.AddAuthorization();

builder.Services.AddSingleton<DbConnectionFactory>();
builder.Services.AddScoped<EmployeeRepository>();
builder.Services.AddScoped<PayRunRepository>();
builder.Services.AddScoped<TimesheetRepository>();
builder.Services.AddScoped<ApiKeyRepository>();

builder.Services.AddSingleton<ISerializationBinder, TimesheetTypeBinder>();
builder.Services.AddScoped<TimesheetImportService>();
builder.Services.AddSingleton<CredentialService>();
builder.Services.AddSingleton<PayslipRenderer>();
builder.Services.AddSingleton<PayslipStore>();
builder.Services.AddSingleton<PayslipLinkService>();
builder.Services.AddSingleton<BankDirectory>();
builder.Services.AddSingleton<BankFileService>();

builder.Services.AddHttpClient<WebhookNotifier>(client => client.Timeout = TimeSpan.FromSeconds(10));
builder.Services.AddHttpClient<BankHolidayClient>(client => client.Timeout = TimeSpan.FromSeconds(10));

if (builder.Environment.IsDevelopment())
{
    builder.Services.AddHttpClient<IBankClient, SandboxBankClient>(client =>
        client.BaseAddress = new Uri("https://sandbox.bankconnect.example"));
}
else
{
    builder.Services.AddHttpClient<IBankClient, BankGatewayClient>(client =>
        client.BaseAddress = new Uri(builder.Configuration["BankGateway:BaseUrl"]!));
}

var app = builder.Build();

app.UseHttpsRedirection();
app.UseAuthentication();
app.UseWhen(
    context => context.Request.Path.StartsWithSegments("/admin"),
    admin => admin.UseMiddleware<AdminAreaMiddleware>());
app.UseAuthorization();
app.MapControllers();

app.Run();
