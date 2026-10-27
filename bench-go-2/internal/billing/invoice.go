package billing

import (
	"fmt"
	"strings"

	"meterhub/internal/store"
)

func InvoiceHeader(b *store.Bill) string {
	var sb strings.Builder
	fmt.Fprintf(&sb, "Invoice #%d\n", b.ID)
	fmt.Fprintf(&sb, "Customer: %s <%s>\n", b.Customer.Name, b.Customer.Email)
	fmt.Fprintf(&sb, "Meter: %d\n", b.MeterID)
	fmt.Fprintf(&sb, "Period: %s - %s\n",
		b.PeriodStart.Format("2006-01-02"), b.PeriodEnd.Format("2006-01-02"))
	return sb.String()
}

func BuildLines(t *store.Tariff, energyCents, standingCents int64) []store.BillLine {
	lines := []store.BillLine{
		{Description: "Energy (" + t.Name + ")", AmountCents: energyCents},
	}
	if standingCents > 0 {
		lines = append(lines, store.BillLine{Description: "Standing charge", AmountCents: standingCents})
	}
	return lines
}

func RenderInvoiceText(b *store.Bill, lines []store.BillLine) string {
	var sb strings.Builder
	sb.WriteString(InvoiceHeader(b))
	sb.WriteString(strings.Repeat("-", 40) + "\n")
	for _, l := range lines {
		fmt.Fprintf(&sb, "%-30s %9.2f\n", l.Description, float64(l.AmountCents)/100)
	}
	fmt.Fprintf(&sb, "%-30s %9.2f\n", "Total", float64(b.AmountCents)/100)
	fmt.Fprintf(&sb, "Energy used: %.3f kWh\n", float64(b.EnergyWh)/1000)
	return sb.String()
}
