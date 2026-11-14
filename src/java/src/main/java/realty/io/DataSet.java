package realty.io;

import java.util.List;

import realty.model.BuyerRequest;
import realty.model.Deal;
import realty.model.DocumentType;
import realty.model.MarketListing;
import realty.model.PropertyObject;

/** Исходные данные: рыночные предложения, объекты агентства, заявки, справочник документов, сделки. */
public record DataSet(List<MarketListing> listings, List<PropertyObject> objects, List<BuyerRequest> requests,
                      List<DocumentType> docTypes, List<Deal> deals) {
}
