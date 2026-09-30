package com.corclan.glow;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Animation;
import net.runelite.api.AnimationController;
import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import net.runelite.api.Player;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.gameval.AnimationID;

/**
 * An upside-down copy of Nex's smoke cloud hanging over each Owner. The game can't flip a spot
 * anim, so this rebuilds it as a client-side object from the same model and animation
 * ({@code nex_mushroom_cloud_spotanim}: model 43213, anim 9201, drawn at 2x) turned over.
 * Client-side only: other players never see it. All methods must run on the client thread.
 */
@Singleton
public class StormCloud
{
	static final int MODEL_ID = 43213;
	static final int ANIMATION_ID = AnimationID.NEX_MUSHROOM_CLOUD_SPOTANIM;
	/** The spot anim draws the model at 256/128 = 2x. */
	static final int SCALE = 256;
	/** Height of the cloud's underside above the ground, in local units (a character is ~200). */
	static final int HEIGHT = 320;
	/** 2 = half the game's animation speed. */
	static final int SLOWDOWN = 2;

	private final Client client;
	private final Map<Player, RuneLiteObject> clouds = new IdentityHashMap<>();
	/** Built once, when the model has loaded from the cache; null until then. */
	private Model model;
	private Animation animation;

	@Inject
	StormCloud(Client client)
	{
		this.client = client;
	}

	/** Keeps exactly one cloud per Owner in view. Call every game tick. */
	public void update(List<Player> owners)
	{
		clouds.entrySet().removeIf(e ->
		{
			if (owners.contains(e.getKey()))
			{
				return false;
			}
			e.getValue().setActive(false);
			return true;
		});
		if (owners.isEmpty() || !loaded())
		{
			return;
		}
		for (Player owner : owners)
		{
			clouds.computeIfAbsent(owner, this::spawn);
		}
	}

	/** Moves each cloud to its Owner. Call every client frame so it follows smoothly. */
	public void follow()
	{
		clouds.forEach((owner, cloud) -> place(cloud, owner));
	}

	public void clear()
	{
		clouds.values().forEach(cloud -> cloud.setActive(false));
		clouds.clear();
	}

	private boolean loaded()
	{
		if (model == null)
		{
			ModelData data = client.loadModelData(MODEL_ID);
			if (data == null)
			{
				// not in the cache yet; try again next tick
				return false;
			}
			model = flip(data.cloneVertices());
			animation = client.loadAnimation(ANIMATION_ID);
		}
		return true;
	}

	/**
	 * Scales to the spot anim's size and turns it upside down, then lights it as the game lights
	 * spot anims. Negating Y and Z is a 180 degree rotation (not a mirror), so faces keep their
	 * winding and stay visible. Afterwards the cloud's old top is its underside, at the origin.
	 */
	private static Model flip(ModelData data)
	{
		data.scale(SCALE, -SCALE, -SCALE);
		// in model space up is -Y; the flipped cloud now hangs below the origin, so lift it by its height
		float lowest = 0;
		for (float y : data.getVerticesY())
		{
			lowest = Math.max(lowest, y);
		}
		data.translate(0, -Math.round(lowest), 0);
		// spot anims are lit with ambient 64 + their own (64) and contrast 850
		return data.light(128, 850, -30, -50, -30);
	}

	private RuneLiteObject spawn(Player owner)
	{
		RuneLiteObject cloud = new SlowObject(client, SLOWDOWN);
		cloud.setModel(model);
		// each cloud has its own controller (its own frame), restarting itself whenever it finishes
		cloud.setAnimationController(new AnimationController(client, animation).setOnFinished(AnimationController::loop));
		place(cloud, owner);
		cloud.setActive(true);
		return cloud;
	}

	private void place(RuneLiteObject cloud, Player owner)
	{
		LocalPoint at = owner.getLocalLocation();
		if (at == null)
		{
			return;
		}
		cloud.setLocation(at, owner.getWorldView().getPlane());
		// setLocation puts it on the ground; Z grows downward, so subtract to raise it
		cloud.setZ(cloud.getZ() - HEIGHT);
	}

	/** A RuneLiteObject whose animation runs {@code slowdown} times slower than the game's. */
	static final class SlowObject extends RuneLiteObject
	{
		private final int slowdown;
		private int pending;

		SlowObject(Client client, int slowdown)
		{
			super(client);
			this.slowdown = slowdown;
		}

		@Override
		public void tick(int ticks)
		{
			pending += ticks;
			int pass = pending / slowdown;
			pending -= pass * slowdown;
			super.tick(pass);
		}
	}
}
