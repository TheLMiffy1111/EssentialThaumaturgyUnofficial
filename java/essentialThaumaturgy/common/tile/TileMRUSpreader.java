package essentialThaumaturgy.common.tile;

import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import DummyCore.Utils.BlockPosition;
import DummyCore.Utils.Coord3D;
import DummyCore.Utils.Lightning;
import DummyCore.Utils.MathUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import ec3.api.EnumStructureType;
import ec3.api.IStructurePiece;
import ec3.api.ITEHasMRU;
import ec3.common.item.ItemsCore;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import thaumcraft.api.aspects.Aspect;

public class TileMRUSpreader extends TileHasMRU {
	
	public List<BlockPosition> devices = new ArrayList();
	
	public List<Lightning> lightnings = new ArrayList();
	
	public Hashtable<BlockPosition, Lightning> lights = new Hashtable();
	
	public int refreshTimer;
	
	public TileMRUSpreader() {
		setMaxMRU(50000F);
		setSlotsNum(1);
	}
	
	public void initDevices() {
		lights.clear();
		devices.clear();
		for(int dx = -16; dx <= 16; ++dx) {
			for(int dy = -16; dy <= 16; ++dy) {
				for(int dz = -16; dz <= 16; ++dz) {
					TileEntity tile = worldObj.getTileEntity(xCoord+dx, yCoord+dy, zCoord+dz);
					if(tile != null && tile != this) {
						try {
							Class TileecController = Class.forName("ec3.common.tile.TileecController");
							if(!(tile.getClass().isAssignableFrom(TileecController))) {
								if((tile instanceof IStructurePiece)) {
									IStructurePiece piece = (IStructurePiece)tile;
									if(piece.getStructure() != EnumStructureType.MRUCUContaigementChamber) {
										if(tile instanceof ITEHasMRU)
											devices.add(new BlockPosition(worldObj, xCoord+dx, yCoord+dy, zCoord+dz));
									}
								}
								else if(tile instanceof ITEHasMRU)
									devices.add(new BlockPosition(worldObj, xCoord+dx, yCoord+dy, zCoord+dz));
							}
						}
						catch(Exception e) {
							e.printStackTrace();
							return;
						}
					}
				}
			}
		}
	}
	
	@Override
	public void updateEntity() {
		super.updateEntity();
		if(refreshTimer <= 0) {
			initDevices();
			refreshTimer = 600;
		}
		else
			--refreshTimer;
		if(worldObj.isRemote && lightnings.size() <= 20) {
			Lightning l = new Lightning(worldObj.rand, new Coord3D(0.5F, 0.5F, 0.5F), new Coord3D(0.5F+MathUtils.randomFloat(worldObj.rand)/2F, 0.5F+MathUtils.randomFloat(worldObj.rand)/2F, 0.5F+MathUtils.randomFloat(worldObj.rand)/2F), 0.01F, 0.7F, 0.0F, 1.0F);
			lightnings.add(l);
		}
		for(BlockPosition b : devices) {
			TileEntity tile = b.blockTile;
			if(tile != null && tile instanceof ITEHasMRU && getMRU() > 0) {
				ITEHasMRU storage = (ITEHasMRU) tile;
				int currentMRU = storage.getMRU();
				int maxMRU = storage.getMaxMRU();
				if(currentMRU < maxMRU && getMRU() >= maxMRU - currentMRU) {
					storage.setMRU(maxMRU);
					setMRU(getMRU() - (maxMRU-currentMRU));
					mruIn();
					if(worldObj.isRemote) {
						Lightning l = new Lightning(worldObj.rand,new Coord3D(0.5F, 0.5F, 0.5F),new Coord3D(tile.xCoord-xCoord+0.5F, tile.yCoord-yCoord+0.5F, tile.zCoord-zCoord+0.5F),0.1F,1,0,1);
						if(!lights.containsKey(b))
							lights.put(b, l);
					}
				}
				else if(currentMRU < maxMRU) {
					storage.setMRU(currentMRU + getMRU());
					setMRU(0);
					mruIn();
					if(worldObj.isRemote) {
						Lightning l = new Lightning(worldObj.rand,new Coord3D(0.5F, 0.5F, 0.5F),new Coord3D(tile.xCoord-xCoord+0.5F, tile.yCoord-yCoord+0.5F, tile.zCoord-zCoord+0.5F),0.1F,1,0,1);
						if(!lights.containsKey(b))
							lights.put(b, l);
					}
				}
			}
			Lightning lt = lights.get(b);
			if(lt != null && lt.renderTicksExisted >= 10)
				lights.remove(b);
		}
		
		if(worldObj.isRemote) {
			for(int i = 0; i < lightnings.size(); ++i) {
				Lightning lt = lightnings.get(i);
				if(lt.renderTicksExisted >= 21)
					lightnings.remove(i);
			}
		}
	}
	
	@Override
	public boolean isItemValidForSlot(int p_94041_1_, ItemStack p_94041_2_) {
		return isBoundGem(p_94041_2_);
	}
}
