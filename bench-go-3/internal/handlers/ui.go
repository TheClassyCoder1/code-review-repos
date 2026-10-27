package handlers

import (
	"fmt"
	"html/template"
	"net/http"
	"strings"

	"github.com/gin-gonic/gin"

	"artifactd/internal/auth"
)

func (h *Handler) RepositoryPage(c *gin.Context) {
	repo := auth.CurrentRepository(c)
	artifacts, err := h.store.ListArtifacts(c.Request.Context(), repo.ID, "pushed_at DESC")
	if err != nil {
		c.String(http.StatusInternalServerError, "failed to load artifacts")
		return
	}
	var rows strings.Builder
	for _, a := range artifacts {
		fmt.Fprintf(&rows, "<tr><td>%s</td><td><code>%s</code></td><td>%s</td></tr>",
			tagBadge(a.Tag), template.HTMLEscapeString(a.Digest), formatSize(a.SizeBytes))
	}
	page := fmt.Sprintf(`<!doctype html><html><head><title>%s/%s</title></head><body>
<h1>%s/%s</h1>
<p class="description">%s</p>
<table><tr><th>Tag</th><th>Digest</th><th>Size</th></tr>%s</table>
</body></html>`, repo.Namespace, repo.Name, repo.Namespace, repo.Name, repo.Description, rows.String())
	c.Data(http.StatusOK, "text/html; charset=utf-8", []byte(page))
}

func formatSize(n int64) string {
	const unit = 1024
	if n < unit {
		return fmt.Sprintf("%d B", n)
	}
	div, exp := int64(unit), 0
	for m := n / unit; m >= unit; m /= unit {
		div *= unit
		exp++
	}
	return fmt.Sprintf("%.1f %ciB", float64(n)/float64(div), "KMGTPE"[exp])
}

func tagBadge(tag string) template.HTML {
	class := "tag"
	if tag == "latest" {
		class = "tag tag-latest"
	}
	return template.HTML(`<span class="` + class + `">` + tag + `</span>`)
}
