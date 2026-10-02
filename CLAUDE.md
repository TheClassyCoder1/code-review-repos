# Engineering guidelines

These rules apply to every service in this repository.

1. Never log personal data (names, emails, phone numbers, national IDs). Log internal IDs only.
2. Controllers must not query the database directly. Go through a service or repository.
3. Use constructor injection. Do not use field injection with `@Autowired`.
4. Public `/api/v1` response shapes are contracts with loan-service and portal-bff. Changing a field name or type requires a new versioned endpoint.
5. Kafka payloads are contracts with their consumers. Do not change a payload's format in place.
