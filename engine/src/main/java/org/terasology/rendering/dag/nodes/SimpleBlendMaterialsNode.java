/*
 * Copyright 2016 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.rendering.dag.nodes;

import org.lwjgl.opengl.GL11;
import org.terasology.engine.ComponentSystemManager;
import org.terasology.entitySystem.systems.RenderSystem;
import org.terasology.monitoring.PerformanceMonitor;
import org.terasology.registry.In;
import org.terasology.rendering.dag.AbstractNode;
import org.terasology.rendering.dag.stateChanges.BindFBO;
import org.terasology.rendering.opengl.FrameBuffersManager;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;

/**
 * TODO: Add diagram of this node
 */
public class SimpleBlendMaterialsNode extends AbstractNode {

    private static final String SCENE_OPAQUE_FBO = "sceneOpaque";

    @In
    private ComponentSystemManager componentSystemManager;

    @In
    private FrameBuffersManager frameBuffersManager;


    @Override
    public void initialise() {
        addDesiredStateChange(new BindFBO(SCENE_OPAQUE_FBO));
        // TODO: review - might be redundant to setRenderBufferMask(sceneOpaque) again at the end of the process() method
    }

    @Override
    public void process() {
        PerformanceMonitor.startActivity("rendering/simpleBlendMaterials");
        preRenderSetupSimpleBlendMaterials();

        for (RenderSystem renderer : componentSystemManager.iterateRenderSubscribers()) {
            renderer.renderAlphaBlend();
        }

        postRenderCleanupSimpleBlendMaterials();
        PerformanceMonitor.endActivity();
    }

    /**
     * Sets the state for the rendering of objects or portions of objects having some degree of transparency.
     * <p>
     * Generally speaking objects drawn with this state will have their color blended with the background
     * color, depending on their opacity. I.e. a 25% opaque foreground object will provide 25% of its
     * color while the background will provide the remaining 75%. The sum of the two RGBA vectors gets
     * written onto the output buffer.
     * <p>
     * Important note: this method disables writing to the Depth Buffer. This is why filters relying on
     * depth information (i.e. DoF) have problems with transparent objects: the depth of their pixels is
     * found to be that of the background. This is an unresolved (unresolv-able?) issue that would only
     * be reversed, not eliminated, by re-enabling writing to the Depth Buffer.
     */
    private void preRenderSetupSimpleBlendMaterials() {
        frameBuffersManager.getFBO(SCENE_OPAQUE_FBO).setRenderBufferMask(true, true, true);
        GL11.glEnable(GL_BLEND);
        GL11.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA); // (*)
        GL11.glDepthMask(false);

        // (*) In this context SRC is Foreground. This effectively says:
        // Resulting RGB = ForegroundRGB * ForegroundAlpha + BackgroundRGB * (1 - ForegroundAlpha)
        // Which might still look complicated, but it's actually the most typical alpha-driven composite.
        // A neat tool to play with this settings can be found here: http://www.andersriggelsen.dk/glblendfunc.php
    }

    /**
     * Resets the state after the rendering of semi-opaque/semi-transparent objects.
     * <p>
     * See preRenderSetupSimpleBlendMaterials() for additional information.
     */
    private void postRenderCleanupSimpleBlendMaterials() {
        GL11.glDisable(GL_BLEND);
        GL11.glDepthMask(true);
    }
}
