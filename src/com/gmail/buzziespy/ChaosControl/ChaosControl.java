package com.gmail.buzziespy.ChaosControl;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
//import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
//import org.bukkit.event.player.PlayerAnimationEvent;
//import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class ChaosControl extends JavaPlugin implements Listener{
	
	private final String EMERALD_NAME = "§aChaos Emerald";
	private final int CHAOSCONTROL_COST = 1;
	private final int CHAOSBLAST_COST = 60;
	
	@Override
	public void onEnable()
	{
		//enable the listener
		getServer().getPluginManager().registerEvents(this, this);
	}
	
	@Override
	public void onDisable()
	{
		
	}
	
	//first, emeralds need to drop - they will drop from villagers
	//only 1% chance for drop
	@EventHandler
	public void onVillagerKill(EntityDeathEvent e)
	{
		//getLogger().info("entity killed - " + e.getEntity().toString());
		if (e.getEntity() instanceof Villager)
		{
			//getLogger().info("player killer!");
			Villager v = (Villager)e.getEntity();
			EntityDamageEvent ee = v.getLastDamageCause();
			
			//can't find a way to check if the player kills the villager - for now this will work
			//for any death by entity (including zombies)
			if (ee.getCause().equals(EntityDamageEvent.DamageCause.ENTITY_ATTACK))
			{
				getLogger().info("attacked by entity");
				if (Math.random() < 0.99)
				{
					ItemStack chaosEmerald = new ItemStack(Material.EMERALD);
					ItemMeta ii = chaosEmerald.getItemMeta();
					ii.setDisplayName(EMERALD_NAME);
					chaosEmerald.setItemMeta(ii);
					e.getEntity().getWorld().dropItemNaturally(v.getLocation(), chaosEmerald);
				}
			}
		}
	}
	
	//implement Chaos Control - works like a compass.  Left click -> teleport to spot in line-of-sight,
	//right click (in creative mode) -> pass through walls and attempt to teleport to a floor on the other side.
	//In survival mode, every teleport costs 1 level.
	//In creative mode, teleports cost nothing - can use to teleport around the map at ease.
	
	//implement Chaos Blast - works like an OMGWTFBBQ.  Right-click -> player inflicted with Weakness for 2
	//seconds and announces in chat the move ("CHAOS...").  Two seconds later, remove Weakness debuff, and
	//unleash a powerful explosion that does not destroy blocks, and conclude announcement ("...BLAST!").
	//This explosion should be powerful enough to kill Prot 4 diamondclads even from a short distance away.
	//Note that before the explosion is unleashed, the player should still be killable; if the player is
	//dead, then the blast is aborted.
	//In survival mode, this costs 50 levels to perform.
	//In creative mode, this is disabled.
	
	//this can probably be handled in PlayerInteractEvent since it can differentiate
	//between left and right clicks
	/*
	@EventHandler
	public void onPowerUse(PlayerAnimationEvent e)
	{
		if (e.getAnimationType().equals(PlayerAnimationType.ARM_SWING))
		{
			getLogger().info("Arm swing!");
		}
		
	}
	*/
	
	//This is used to keep the player alive during the explosion
	@EventHandler
	public void onHurtByExplosion(EntityDamageEvent e)
	{
		if (e.getEntity().hasMetadata("ChaosControl.Blast") && e.getCause().equals(EntityDamageEvent.DamageCause.BLOCK_EXPLOSION))
		{
			e.getEntity().removeMetadata("ChaosControl.Blast", this);
			e.getEntity().removeMetadata("ChaosControl.BlastProcess", this);
			e.setCancelled(true);
		}
	}
	
	
	//Toggle between Chaos Control and Chaos Blast
	//right-click with the emerald in hand
	@EventHandler
	public void onPowerToggle(PlayerInteractEvent e)
	{
		if (hasChaosEmerald(e.getPlayer()))
		{
			//for right-clicking: in S mode, toggle chaos emerald power
			//in C mode, check if there is empty space on other side of a wall, then teleport player to it
			if (e.getAction().equals(Action.RIGHT_CLICK_AIR) || e.getAction().equals(Action.RIGHT_CLICK_BLOCK))
			{
				if (e.getPlayer().getGameMode().equals(GameMode.SURVIVAL) || e.getPlayer().getGameMode().equals(GameMode.ADVENTURE))
				{
					togglePower(e.getPlayer());
				}
				else
				{
					if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK))
					{
						BlockFace bf = e.getBlockFace();
						Block otherSide = e.getClickedBlock().getRelative(bf.getOppositeFace().getModX(), bf.getOppositeFace().getModY(), bf.getOppositeFace().getModZ());
						float yaw = e.getPlayer().getLocation().getYaw();
						float pitch = e.getPlayer().getLocation().getPitch();
						//for now, only teleport to an air location, though can probably alter this to include stuff like signs or redstone wires
						
						if (otherSide.getType().equals(Material.AIR))
						{
							//place player on the ground
							Block landZone = otherSide;
							while (landZone.getRelative(0, -1, 0).isEmpty())
							{
								landZone = landZone.getRelative(0, -1, 0);
							}
							e.getPlayer().teleport(new Location(e.getPlayer().getWorld(), (double)landZone.getX(), (double)landZone.getY(), (double)landZone.getZ(), yaw, pitch));
						}
						else
						{
							//keep checking the blocks on the other side for an empty spot to teleport to
							Block warpTo = otherSide;
							int attempts = 10;
							while (!warpTo.isEmpty())
							{
								if (warpTo.getY() >= 255 || warpTo.getY() <= 1 || attempts == 0)
								{
									e.getPlayer().sendMessage(ChatColor.RED + "Cannot find an empty space on the other side!");
									return;
								}
								warpTo = otherSide.getRelative(bf.getOppositeFace().getModX(), bf.getOppositeFace().getModY(), bf.getOppositeFace().getModZ());
								attempts--;
							}
							Block landZone = warpTo;
							while (landZone.getRelative(0, -1, 0).isEmpty())
							{
								landZone = landZone.getRelative(0, -1, 0);
							}
							e.getPlayer().teleport(new Location(e.getPlayer().getWorld(), (double)landZone.getX(), (double)landZone.getY(), (double)landZone.getZ(), yaw, pitch));
						
						}
					}
					
				}
				
				/*
				List<Block> sight = e.getPlayer().getLineOfSight(null, 5);
				for (Block b: sight)
				{
					getLogger().info("" + b.getType().toString());
				}
				*/
			}
			else if (e.getAction().equals(Action.LEFT_CLICK_AIR) || e.getAction().equals(Action.LEFT_CLICK_BLOCK))
			{
				//left-click in S mode: trigger Emerald power:
				//Chaos control: teleport to a block in line of sight.  -1 level for each use.
				//Chaos blast: Big explosion centered on player, no block damage but damage all entites around player.
				//-60 levels for each use.
				//in C mode: teleport to a block in line of sight (Chaos Control), no level subtraction
				if (e.getPlayer().getGameMode().equals(GameMode.SURVIVAL) || e.getPlayer().getGameMode().equals(GameMode.ADVENTURE))
				{
					if (e.getPlayer().hasMetadata("ChaosControl.Mode")) //for now, if metadata exists, player is in Blast mode
					{
						if (e.getPlayer().getLevel() < CHAOSBLAST_COST)
						{
							e.getPlayer().sendMessage(ChatColor.RED + "You do not have enough XP levels to use Chaos Blast!");
							return;
						}
						
						//if a blast is in progress, do nothing
						if (e.getPlayer().hasMetadata("ChaosControl.BlastProcess"))
						{
							e.getPlayer().sendMessage(ChatColor.RED + "Your Chaos Blast is already in process!");
							return;
						}
						
						//prepare the blast when it actually occurs
						final Player p = e.getPlayer();
						final FixedMetadataValue blastdata = new FixedMetadataValue(this, "true");
						final Server s = e.getPlayer().getServer();
						Runnable blast = new Runnable()
						{
							public void run()
							{
								//Chaos Blast
								if (p.isDead())
								{
									//abort blast if player is dead by the time blast is meant to occur
									return;
								}
								double x = p.getLocation().getX();
								double y = p.getLocation().getY();
								double z = p.getLocation().getZ();
								
								s.broadcastMessage("<" + p.getName() + "> " + ChatColor.RED + "BLAST!");
								
								//Explosion power - needs to be strong enough to kill someone in Prot 4 dia at close range
								//TNT has explosion power of 4
								p.setMetadata("ChaosControl.Blast", blastdata);
								p.getWorld().createExplosion(x, y, z, 40, false, false);
							}
						};
						
						//Build-up to the blast
						//inflict slowness 2 on player for 2 seconds and announce 
						p.setLevel(p.getLevel() - CHAOSBLAST_COST);
						p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 2));
						p.setMetadata("ChaosControl.BlastProcess", blastdata);
						s.broadcastMessage("<" + p.getName() + "> " + ChatColor.RED + "Chaos...");
						
						//unleash blast 2 seconds later
						Bukkit.getScheduler().scheduleSyncDelayedTask(this, blast, 40L);
					}
					else
					{
						//Chaos Control - can probably just write one helper method for this?
						Block target = e.getPlayer().getTargetBlock(null, 100);
						//If player targets something out of range (air is returned), do nothing
						if (target.getType().equals(Material.AIR))
						{
							return;
						}
						ChaosTeleport(e.getPlayer(), target);
					}
				}
				else //if player is in Creative
				{
					//Chaos Control
					Block target = e.getPlayer().getTargetBlock(null, 100);
					ChaosTeleport(e.getPlayer(), target);
				}
				
			}
		}
		
	}
	
	//If item in hand is the Chaos Emerald, cancel any block breaking
	@EventHandler
	public void onPowerUseBreak(BlockBreakEvent e)
	{
		//getLogger().info("BlockDamageEvent");
		try
		{
		if (e.getPlayer().getItemInHand().hasItemMeta())
		{
			if (e.getPlayer().getItemInHand().getItemMeta().getDisplayName().equals("§aChaos Emerald"))
			{
				e.setCancelled(true);
			}
		}
		}
		catch (RuntimeException re)
		{
			re.printStackTrace();
		}
	}
	
	public void togglePower(Player p)
	{
		if (hasChaosEmerald(p))
		{
			if (!p.hasMetadata("ChaosControl.Mode"))
			{
				p.setMetadata("ChaosControl.Mode", new FixedMetadataValue(this, "blast"));
				p.sendMessage(ChatColor.GREEN + "Chaos Blast: 60 levels to unleash a powerful explosion.");
			}
			else //if the mode metadata has been set
			{
				p.removeMetadata("ChaosControl.Mode", this);
				p.sendMessage(ChatColor.GREEN + "Chaos Control: 1 level to teleport to a place in your line of sight.");
			}
		}	
	}
	
	public boolean hasChaosEmerald(Player p)
	{
		if (p.getItemInHand().hasItemMeta() && p.getItemInHand().getType().equals(Material.EMERALD))
		{
			if (p.getItemInHand().getItemMeta().getDisplayName().equals(EMERALD_NAME))
			{
				return true;
			}
		}
		return false;
	}
	
	public void ChaosTeleport(Player p, Block target)
	{
		//Chaos Control
		
		if (p.getGameMode().equals(GameMode.SURVIVAL) || p.getGameMode().equals(GameMode.ADVENTURE))
		{
			if (p.getLevel() < 1)
			{
				p.sendMessage(ChatColor.RED + "You do not have enough XP levels to use Chaos Control!");
				return;
			}
		}
		
		if (target.isEmpty())
		{
			//do nothing if target block is air
			return;
		}
		//check space on top
		//Block targetSurface = target;
		
		while (((p.getGameMode().equals(GameMode.SURVIVAL) || p.getGameMode().equals(GameMode.ADVENTURE)) && !target.getRelative(0, 1, 0).isEmpty()) || (p.getGameMode().equals(GameMode.CREATIVE) && !(target.getRelative(0, 1, 0).isEmpty() && target.getRelative(0, 2, 0).isEmpty())))
		//while ((e.getPlayer().getGameMode().equals(GameMode.CREATIVE) && !(target.getRelative(0, 1, 0).isEmpty() && target.getRelative(0, 2, 0).isEmpty())))
		{
			target = target.getRelative(0, 1, 0);
			if (target.getY() > 255)
			{
				p.sendMessage("This will put you over the world limit!");
				return;
			}
		}
		target = target.getRelative(0, 1, 0);
		float pitch = p.getLocation().getPitch();
		float yaw = p.getLocation().getYaw();
		
		Location newPlace = new Location(p.getWorld(), target.getX(), target.getY(), target.getZ(), yaw, pitch);
		//play sound if player is in S mode
		//at player teleport location and at target location
		if (p.getGameMode().equals(GameMode.SURVIVAL) || p.getGameMode().equals(GameMode.ADVENTURE))
		{
			p.setLevel(p.getLevel()-CHAOSCONTROL_COST);
			p.getWorld().playSound(p.getLocation(), Sound.ENDERMAN_TELEPORT, 2F, 1F);
			p.getWorld().playSound(newPlace, Sound.ENDERMAN_TELEPORT, 2F, 1F);
		}
		
		//teleport
		p.teleport(newPlace);
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		if (cmd.getName().equalsIgnoreCase("givechaos"))
		{
			if (sender instanceof Player)
			{
				ItemStack chaosEmerald = new ItemStack(Material.EMERALD);
				ItemMeta ii = chaosEmerald.getItemMeta();
				ii.setDisplayName(EMERALD_NAME);
				chaosEmerald.setItemMeta(ii);
				
				Player p = (Player)sender;
				p.getWorld().dropItem(p.getLocation(), chaosEmerald);
				return true;
			}
		}
		return false;
	}
}
