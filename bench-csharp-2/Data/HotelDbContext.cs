using Hotel.Models;
using Microsoft.EntityFrameworkCore;

namespace Hotel.Data;

public class HotelDbContext : DbContext
{
    public HotelDbContext(DbContextOptions<HotelDbContext> options) : base(options) { }

    public DbSet<Guest> Guests => Set<Guest>();
    public DbSet<Room> Rooms => Set<Room>();
    public DbSet<RatePlan> RatePlans => Set<RatePlan>();
    public DbSet<Booking> Bookings => Set<Booking>();
    public DbSet<Folio> Folios => Set<Folio>();
    public DbSet<FolioLine> FolioLines => Set<FolioLine>();

    protected override void OnModelCreating(ModelBuilder model)
    {
        model.Entity<Guest>().HasIndex(g => new { g.HotelId, g.Email });

        model.Entity<Room>().HasIndex(r => new { r.HotelId, r.Number }).IsUnique();
        model.Entity<Room>().Property(r => r.Version).IsRowVersion();

        model.Entity<RatePlan>().HasIndex(p => new { p.HotelId, p.Code }).IsUnique();
        model.Entity<RatePlan>().Property(p => p.NightlyRate).HasPrecision(12, 2);

        model.Entity<Booking>().HasIndex(b => b.ConfirmationCode).IsUnique();
        model.Entity<Booking>()
            .HasOne(b => b.Guest)
            .WithMany()
            .HasForeignKey(b => b.GuestId)
            .IsRequired();
        model.Entity<Booking>().Property(b => b.TotalAmount).HasPrecision(12, 2);

        model.Entity<Folio>().HasIndex(f => f.BookingId).IsUnique();
        model.Entity<Folio>().Property(f => f.Balance).HasPrecision(12, 2);
        model.Entity<Folio>().Property(f => f.Version).IsRowVersion();
        model.Entity<FolioLine>().Property(l => l.Amount).HasPrecision(12, 2);
    }
}
