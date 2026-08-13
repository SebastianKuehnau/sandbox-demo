# Data Model

> Entity definitions and relationships. Evolves as features are added.

| Entity | Key Fields | Relationships |
|--------|-----------|------------------|
| Talk | id, title, description, speakerName, type (PRESENTATION or WORKSHOP), scheduledDate, duration (minutes), location | Standalone |

---

## Field Details

### Talk

- **id**: Unique identifier (auto-generated)
- **title**: Name of the presentation or workshop (required, text)
- **description**: Detailed information about the talk (required, text/long-form)
- **speakerName**: Name of the speaker or facilitator (required, text)
- **type**: Talk category — either PRESENTATION or WORKSHOP (required, enum)
- **scheduledDate**: Date and time when the talk occurs (required, datetime)
- **duration**: Length of the talk in minutes (required, integer)
- **location**: Physical or virtual location where the talk takes place (required, text)
