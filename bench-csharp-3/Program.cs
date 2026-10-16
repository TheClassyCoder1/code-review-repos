using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.IdentityModel.Tokens;
using PolicyAdmin.Data;
using PolicyAdmin.Endpoints;
using PolicyAdmin.Security;
using PolicyAdmin.Services;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddControllersWithViews();
builder.Services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
    .AddJwtBearer(options =>
    {
        options.TokenValidationParameters = new TokenValidationParameters
        {
            ValidIssuer = builder.Configuration["Jwt:Issuer"],
            ValidAudience = builder.Configuration["Jwt:Audience"],
            IssuerSigningKey = TokenService.CreateSigningKey()
        };
    });
builder.Services.AddAuthorization();

builder.Services.AddSingleton<PolicyRepository>();
builder.Services.AddSingleton<EndorsementRepository>();
builder.Services.AddSingleton<PasswordService>();
builder.Services.AddSingleton<TokenService>();
builder.Services.AddSingleton<AttachmentStore>();
builder.Services.AddSingleton<DocumentService>();
builder.Services.AddSingleton<EndorsementImportService>();
builder.Services.AddSingleton<QuoteService>();
builder.Services.AddSingleton<PaymentService>();
builder.Services.AddSingleton<DemoDataSeeder>();
builder.Services.AddSingleton<CarrierClient>();
builder.Services.AddScoped<AgentOwnsPolicyFilter>();

builder.Services.AddTransient<AllowedHostsHandler>();
builder.Services.AddHttpClient(CarrierClient.CarrierHttpClient, client => client.Timeout = TimeSpan.FromSeconds(20))
    .AddHttpMessageHandler<AllowedHostsHandler>();
builder.Services.AddHttpClient(CarrierClient.DocumentsHttpClient, client => client.Timeout = TimeSpan.FromSeconds(30));

var app = builder.Build();

if (app.Environment.IsDevelopment())
{
    using var scope = app.Services.CreateScope();
    await scope.ServiceProvider.GetRequiredService<DemoDataSeeder>().SeedDemoAgentAsync(CancellationToken.None);
}

app.UseHttpsRedirection();
app.UseStaticFiles();
app.UseRouting();
app.UseAuthentication();
app.UseAuthorization();

app.MapControllers();
app.MapPolicyEndpoints();

app.Run();
