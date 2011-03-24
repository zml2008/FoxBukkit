package de.doridian.yiffbukkit.commands;

import net.minecraft.server.EntityFallingSand;
import net.minecraft.server.EntityPig;
import net.minecraft.server.EntityTNTPrimed;
import net.minecraft.server.WorldServer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftFallingSand;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.entity.CraftTNTPrimed;
import org.bukkit.entity.Boat;
import org.bukkit.entity.CreatureType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import de.doridian.yiffbukkit.YiffBukkit;
import de.doridian.yiffbukkit.YiffBukkitCommandException;

public class ThrowCommand extends ICommand {
	public ThrowCommand(YiffBukkit plug) {
		super(plug);
	}

	@Override
	public int GetMinLevel() {
		return 4;
	}

	@Override
	public void Run(final Player ply, String[] args, String argStr) throws YiffBukkitCommandException {
		Material toolType = ply.getItemInHand().getType();

		if (argStr.isEmpty()) {
			playerHelper.addToolMapping(ply, toolType, null);

			playerHelper.SendDirectedMessage(ply, "Unbound your current tool ("+toolType.name()+").");

			return;
		}


		double speed = 2;
		if (args.length >= 2) {
			try {
				speed = Double.valueOf(args[1]);
			} catch (NumberFormatException e) {
				throw new YiffBukkitCommandException("Number expected", e);
			}
		}
		final double finalSpeed = speed;

		String typeName = args[0].toUpperCase();

		Runnable runnable;
		if (typeName.equals("ME")) {
			runnable = new Runnable() {
				public void run() {
					Location location = ply.getLocation();

					if (ply.isInsideVehicle()) {
						Entity vehicle = ((CraftPlayer)ply).getHandle().vehicle.getBukkitEntity();//ply.getVehicle()
						vehicle.setVelocity(location.getDirection().multiply(finalSpeed));
					}
					else {
						ply.setVelocity(location.getDirection().multiply(finalSpeed));
					}
				}
			};
		}
		else {
			final String[] types = typeName.split(":");

			runnable = new Runnable() {
				public void run() {
					Entity previous = null;
					final World world = ply.getWorld();
					final WorldServer notchWorld = ((CraftWorld)world).getHandle();
					final Location location = ply.getEyeLocation();
					for (String part : types) {
						Entity entity;
						if (part.equals("ME")) {
							entity = ply;
						}
						else if (part.equals("TNT")) {
							EntityTNTPrimed notchEntity = new EntityTNTPrimed(notchWorld, location.getX(), location.getY(), location.getZ());
							notchWorld.a(notchEntity);

							entity = new CraftTNTPrimed((CraftServer)plugin.getServer(), notchEntity);
						}
						else if(part.equals("SAND") || part.equals("GRAVEL")) {
							int material = Material.valueOf(part).getId();
							EntityFallingSand notchEntity = new EntityFallingSand(notchWorld, location.getX(), location.getY(), location.getZ(), material);
							//EntityTNTPrimed notchEntity = new EntityTNTPrimed(notchWorld, location.getX(), location.getY(), location.getZ());
							notchWorld.a(notchEntity);

							entity = new CraftFallingSand((CraftServer)plugin.getServer(), notchEntity);
						}
						else if (part.equals("MINECART") || part.equals("CART")) {
							entity = world.spawnMinecart(location);
						}
						else if (part.equals("BOAT")) {
							entity = world.spawnBoat(location);
						}
						else if (part.equals("THIS")) {

							Vector eyeVector = location.getDirection().clone();
							Vector eyeOrigin = location.toVector().clone();

							entity = null;
							for (Entity currentEntity : ply.getWorld().getEntities()) {
								Location eyeLocation;
								if (currentEntity instanceof LivingEntity) {
									eyeLocation = ((LivingEntity)currentEntity).getEyeLocation();
								}
								else if (currentEntity instanceof Boat || currentEntity instanceof Minecart) {
									eyeLocation = currentEntity.getLocation();
								}
								else {
									continue;
								}

								Vector pos = eyeLocation.toVector().clone();
								pos.add(new Vector(0, 0.6, 0));

								pos.subtract(eyeOrigin);

								if (pos.lengthSquared() > 9)
									continue;

								double dot = pos.clone().normalize().dot(eyeVector);

								if (dot < 0.8)
									continue;


								if (currentEntity.equals(ply))
									continue;

								entity = currentEntity;
								break;
							}
							if (entity == null) {
								playerHelper.SendDirectedMessage(ply, "You must face a creature/boat/minecart");
								return;
							}
								
						}
						else {
							try {
								CreatureType type = CreatureType.valueOf(part);
								entity = world.spawnCreature(location, type);
							}
							catch (IllegalArgumentException e) {
								playerHelper.SendDirectedMessage(ply, "Creature type "+part+" not found");
								return;
							}
						}

						if (entity == null) {
							playerHelper.SendDirectedMessage(ply, "Failed to spawn "+part);
							return;
						}

						if (previous == null) {
							entity.setVelocity(location.getDirection().multiply(finalSpeed));
						}
						else {
							net.minecraft.server.Entity eCreature = ((CraftEntity)entity).getHandle();
							net.minecraft.server.Entity ePrevious = ((CraftEntity)previous).getHandle();
							if (ePrevious instanceof EntityPig)
								((EntityPig)ePrevious).a(true);

							eCreature.setPassengerOf(ePrevious);
						}

						previous = entity;
					}
				}
			};
		}

		playerHelper.addToolMapping(ply, toolType, runnable);

		playerHelper.SendDirectedMessage(ply, "Bound "+typeName+" to your current tool ("+toolType.name()+"). Right-click to use.");
	}

	@Override
	public String GetHelp() {
		return "Binds creature/tnt/sand/gravel/minecart/self('me')/target('this') throwing to your current tool. Right-click to use. Unbind by typing '/throw' without arguments.";
	}

	@Override
	public String GetUsage() {
		return "[<type> [<speed>]]";
	}
}
