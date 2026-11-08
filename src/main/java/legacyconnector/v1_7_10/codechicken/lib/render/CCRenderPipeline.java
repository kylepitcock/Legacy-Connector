package legacyconnector.v1_7_10.codechicken.lib.render;

import java.util.ArrayList;
import java.util.Comparator;

/** The operations run for each vertex, ordered by id; they are kept only when they say (load) they apply to the model. */
public class CCRenderPipeline {
    private final ArrayList<CCRenderState.IVertexOperation> requested = new ArrayList<>();
    private final ArrayList<CCRenderState.IVertexOperation> active = new ArrayList<>();
    private final ArrayList<CCRenderState.VertexAttribute<?>> dependencies = new ArrayList<>();

    /** Collects operations in the order of calls; {@link #render()} runs them. */
    public class PipelineBuilder {
        public PipelineBuilder add(CCRenderState.IVertexOperation op) {
            requested.add(op);
            return this;
        }

        public PipelineBuilder add(CCRenderState.IVertexOperation... ops) {
            for (CCRenderState.IVertexOperation op : ops) requested.add(op);
            return this;
        }

        public CCRenderPipeline build() {
            return CCRenderPipeline.this;
        }

        public void render() {
            CCRenderState.render();
        }
    }

    public void reset() {
        requested.clear();
        active.clear();
        dependencies.clear();
    }

    public PipelineBuilder builder() {
        reset();
        CCRenderState.pipeline = this;
        return new PipelineBuilder();
    }

    public void setPipeline(CCRenderState.IVertexOperation... ops) {
        for (CCRenderState.IVertexOperation op : ops) requested.add(op);
    }

    /** An operation needs the attribute computed first. */
    public void addDependency(CCRenderState.VertexAttribute<?> attr) {
        if (!dependencies.contains(attr)) dependencies.add(attr);
    }

    public void addAttribute(CCRenderState.VertexAttribute<?> attr) {
        addDependency(attr);
    }

    /** Resolves the requested operations against the current model. */
    public void rebuild() {
        active.clear();
        CCRenderState.hasNormal = false;
        CCRenderState.hasColour = false;
        ArrayList<CCRenderState.IVertexOperation> all = new ArrayList<>();
        if (CCRenderState.useNormals) all.add(CCRenderState.normalAttrib);
        if (CCRenderState.useColour && CCRenderState.computeLighting) all.add(CCRenderState.colourAttrib);
        all.add(CCRenderState.lightCoordAttrib);
        all.add(CCRenderState.sideAttrib);
        all.addAll(requested);
        for (CCRenderState.IVertexOperation op : all) {
            if (op.load()) active.add(op);
        }
        for (CCRenderState.VertexAttribute<?> d : new ArrayList<>(dependencies)) {
            if (!active.contains(d) && d.load()) active.add(d);
        }
        active.sort(Comparator.comparingInt(CCRenderState.IVertexOperation::operationID));
    }

    public void operate() {
        for (int i = 0; i < active.size(); i++) active.get(i).operate();
    }

    public void render() {
        CCRenderState.render();
    }
}
