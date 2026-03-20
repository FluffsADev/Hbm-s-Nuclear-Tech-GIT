package com.hbm.render.tileentity;

import org.lwjgl.opengl.GL11;

import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.storage.TileEntityCombatDropPod;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;

public class RenderCombatPod extends TileEntitySpecialRenderer {
	@Override
	public void renderTileEntityAt(TileEntity te, double x, double y, double z, float i) {
	TileEntityCombatDropPod pod = (TileEntityCombatDropPod) te;	
        GL11.glPushMatrix();
        GL11.glTranslated(x + 0.5, y, z + 0.5);
        GL11.glEnable(GL11.GL_LIGHTING);

        GL11.glTranslatef(0.0F, -0.25F, 0.0F);
        GL11.glRotatef(-25, 0, 1, 0);
        GL11.glRotatef(15, 0, 0, 1);
        
        int color = pod.color;
        
        switch(color) {
        case 0:
             bindTexture(ResourceManager.combat_pod_skin_white);
             break;
        case 1:
            bindTexture(ResourceManager.combat_pod_skin_red);
            break;
        case 2:
            bindTexture(ResourceManager.combat_pod_skin_yellow);
            break;
        default:
            bindTexture(ResourceManager.combat_pod_skin_white);
            break;
        }
        
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        ResourceManager.combat_pod.renderAll();
        GL11.glShadeModel(GL11.GL_FLAT);
        
        GL11.glPopMatrix();
	}

}
