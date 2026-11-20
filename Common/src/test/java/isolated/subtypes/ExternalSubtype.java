package isolated.subtypes;

import isolated.subtypebase.ExternalBase;
import testjackson.jackson.annotation.JsonTypeName;

@JsonTypeName("ext")
public class ExternalSubtype implements ExternalBase {
}
