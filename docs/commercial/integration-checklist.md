# Integration Checklist

## Required Customer Systems

- Identity provider or token service
- Workflow source system
- Document processing source or document repository
- Payment or operational action system, if in pilot scope
- Compliance, audit, or reporting destination
- Optional event broker or message stream

## Required APIs

- Authentication and token validation
- Workflow initiation or event ingestion
- Document metadata or document upload
- Payment or operational action API
- Evidence query and reporting API
- Health and operational readiness endpoints

## Identity Provider Requirements

- JWT issuance
- Scope or role mapping
- Tenant identifier strategy
- Token expiration and refresh expectations
- Service-to-service authentication pattern
- Test users or pilot service accounts

## Event And Message Requirements

- Event names and schemas
- Tenant ID field
- Correlation ID field
- Idempotency key field, where relevant
- Timestamp conventions
- Failure event conventions
- Replay and duplicate event behavior

## Data Retention Questions

- How long must evidence records be retained?
- Which payload fields are sensitive?
- What must be masked, hashed, or excluded?
- Are audit exports required?
- What deletion or legal hold policies apply?
- Which teams can access evidence records?

## Deployment Model Questions

- Customer cloud, vendor cloud, or hybrid?
- Required regions and data residency constraints?
- Required network controls and private connectivity?
- Container orchestration requirements?
- Database and message broker preferences?
- Monitoring, logging, and alerting requirements?

## Compliance Reporting Requirements

- Required evidence package format
- Required audit fields
- Required workflow status definitions
- Reporting frequency
- Export requirements
- Reviewer personas and access model
- Applicable frameworks such as SOC 2, ISO, PCI, HIPAA, or internal risk controls
