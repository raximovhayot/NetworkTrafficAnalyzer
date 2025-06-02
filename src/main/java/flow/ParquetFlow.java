package flow;

/**
 * A specialized Flow class that can be initialized with pre-calculated features.
 * This is used for reading flows from parquet files.
 */
public class ParquetFlow extends Flow {
    private final FlowFeatures features;

    /**
     * Create a new ParquetFlow with the given features
     * @param features the pre-calculated features
     */
    public ParquetFlow(FlowFeatures features) {
        this.features = features;
    }

    /**
     * Override the getFeatures method to return the pre-calculated features
     * @return the pre-calculated features
     */
    @Override
    public synchronized FlowFeatures getFeatures() {
        return features;
    }
}