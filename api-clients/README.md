# API clients

Import [Admin R1.1](postman/MM-CONNECT-Admin-R1.1.postman_collection.json) into
Postman. The collection contains no credentials. Keep populated environments out
of Git. Read the [verification API handoff](../agentkit/context/admin-verification-api.md)
before running review decisions.

Staff provisioning is private; Firebase sign-in does not grant a staff role.
Choose approve OR request-changes for each inspected revision. Do not run the
whole collection linearly. The existing Flutter collection under ignored `docs/`
is a separate local artifact; this collection covers the new staff APIs and status.
