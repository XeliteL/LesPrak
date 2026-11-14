package realty.documents;

/** Замечание по сделке: вид и предмет (название документа или этапа). */
public record Issue(IssueKind kind, String subject) {
}
