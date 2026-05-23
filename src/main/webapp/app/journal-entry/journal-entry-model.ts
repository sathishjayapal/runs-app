export class JournalEntryDTO {

    id?: number | null;
    activityId?: string | null;
    entryDate?: string | null;
    perceivedEffort?: number | null;
    feel?: string | null;
    bodyNotes?: string | null;
    contextNotes?: string | null;
    narrative?: string | null;
    embedded?: boolean | null;
    embeddedAt?: string | null;
    createdAt?: string | null;
    updatedAt?: string | null;

    constructor(data: Partial<JournalEntryDTO>) {
        Object.assign(this, data);
    }

}