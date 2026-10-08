package com.corclan.glow;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Animation;
import net.runelite.api.AnimationController;
import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import net.runelite.api.Player;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.coords.LocalPoint;

/**
 * A game graphic recolored and kept on each wearer. Spot anims can't be recolored, so like {@link StormCloud}
 * this rebuilds one as a client-side object from the same model and animation (taken from the cache's spot
 * anim definitions, see {@link #CATALOG}), with every face turned to one {@link Tint}. It can also drift
 * upwards on a loop. Client-side only: other players never see it. All instance methods run on the client thread.
 */
public class TintedFx
{
	public enum Tint
	{
		/** deep red, keeping the graphic's light and shade */
		RED,
		/** near black, a hint of the shading left so it still has shape */
		BLACK
	}

	/** A spot anim's model, animation, size and lighting, as the cache defines it. */
	static final class Def
	{
		final int spotId;
		final String name;
		final int model;
		final int animation;
		/** 128 = the model's own size */
		final int resizeH;
		final int resizeV;
		final int ambient;
		final int contrast;

		Def(int spotId, String name, int model, int animation, int resizeH, int resizeV, int ambient, int contrast)
		{
			this.spotId = spotId;
			this.name = name;
			this.model = model;
			this.animation = animation;
			this.resizeH = resizeH;
			this.resizeV = resizeV;
			this.ambient = ambient;
			this.contrast = contrast;
		}
	}

	/** Graphics ::dayfx can switch to, by spot anim id. Fires first, then swirls. */
	static final Map<Integer, Def> CATALOG;

	static
	{
		Map<Integer, Def> defs = new LinkedHashMap<>();
		// Tormented Demons (Lucien's undead demons in the cache)
		add(defs, new Def(2852, "Tormented Demon explosion fire", 53283, 11400, 128, 128, 15, 100));
		add(defs, new Def(2849, "Tormented Demon shield", 53283, 11399, 128, 128, 15, 100));
		add(defs, new Def(2850, "Tormented Demon shield restore", 53283, 11401, 128, 128, 15, 100));
		add(defs, new Def(2847, "Tormented Demon summon", 53283, 11402, 128, 128, 15, 100));
		add(defs, new Def(2848, "Tormented Demon death", 53282, 11403, 128, 128, 15, 100));
		add(defs, new Def(2855, "Tormented Demon large fireball", 54031, 11405, 160, 160, 15, 100));
		// other fires
		add(defs, new Def(78, "Flames of Zamorak", 2267, 901, 128, 128, 75, 75));
		add(defs, new Def(2582, "Forestry campfire", 40988, 10574, 128, 128, 10, 100));
		add(defs, new Def(3741, "Flames of Cerberus", 60214, 13756, 128, 128, 10, 60));
		add(defs, new Def(3280, "Yama fire immunity", 14824, 12176, 128, 128, 30, 100));
		// swirls
		add(defs, new Def(1394, "Wyvern cyclone", 33883, 7655, 128, 128, 0, 0));
		add(defs, new Def(56, "Vanguard swirl (short)", 39582, 8643, 126, 96, 20, 156));
		add(defs, new Def(57, "Vanguard swirl (tall)", 39582, 8643, 126, 150, 20, 156));
		add(defs, new Def(1177, "Pest Control spiral", 27160, 6888, 128, 128, 20, 0));
		add(defs, new Def(1296, "Arceuus teleport swirl", 31002, 7162, 128, 128, 20, 0));
		add(defs, new Def(2507, "Leviathan tornado", 35398, 8100, 128, 128, 50, 50));
		add(defs, new Def(3587, "Beef tornado", 59402, 13802, 128, 128, 0, 0));
		add(defs, new Def(642, "Smoke devil cloud", 28443, 3857, 128, 128, 0, 0));
		add(defs, new Def(1488, "Rune dragon electro-vortex", 14557, 3912, 128, 128, 0, 0));
		CATALOG = Collections.unmodifiableMap(defs);
	}

	private static void add(Map<Integer, Def> defs, Def def)
	{
		defs.put(def.spotId, def);
	}

	/**
	 * One face color (the game's 16-bit HSL: 6 bits hue, 3 saturation, 7 lightness) turned to {@code tint}.
	 * Lightness is kept, so the graphic keeps its shading.
	 */
	static short tint(short hsl, Tint tint)
	{
		int lum = hsl & 127;
		switch (tint)
		{
			case RED:
				// hue 0 is red; capping the lightness stops the hot core washing out to pink
				return (short) (7 << 7 | Math.min(lum, 90));
			case BLACK:
				return (short) (lum / 6);
			default:
				return hsl;
		}
	}

	private final Client client;
	private final Tint tint;
	private final Map<Player, RuneLiteObject> objects = new IdentityHashMap<>();
	private Def def;
	/** how far it climbs each loop, in local units (a character is ~200); 0 stays put */
	private int rise;
	private long riseMillis;
	/** Built once the model has loaded from the cache; null until then. */
	private Model model;
	private Animation animation;

	/**
	 * @param rise       how far it climbs each loop, in local units; 0 stays at the feet
	 * @param riseMillis how long one climb takes
	 */
	TintedFx(Client client, Tint tint, int spotId, int rise, long riseMillis)
	{
		this.client = client;
		this.tint = tint;
		this.def = CATALOG.get(spotId);
		this.rise = rise;
		this.riseMillis = riseMillis;
	}

	/** Uses another graphic from {@link #CATALOG} (and climb height) from now on. False if it isn't listed. */
	boolean use(int spotId, int rise)
	{
		Def next = CATALOG.get(spotId);
		if (next == null)
		{
			return false;
		}
		clear();
		def = next;
		this.rise = rise;
		model = null;
		animation = null;
		return true;
	}

	int spotId()
	{
		return def.spotId;
	}

	/** Keeps exactly one object on each of {@code wearers}, following them. Call every client frame. */
	void update(List<Player> wearers)
	{
		objects.entrySet().removeIf(e ->
		{
			if (wearers.contains(e.getKey()))
			{
				return false;
			}
			e.getValue().setActive(false);
			return true;
		});
		if (wearers.isEmpty() || !loaded())
		{
			return;
		}
		long now = System.currentTimeMillis();
		for (Player wearer : wearers)
		{
			place(objects.computeIfAbsent(wearer, this::spawn), wearer, now);
		}
	}

	void clear()
	{
		objects.values().forEach(object -> object.setActive(false));
		objects.clear();
	}

	private boolean loaded()
	{
		if (model == null)
		{
			ModelData data = client.loadModelData(def.model);
			if (data == null)
			{
				// not in the cache yet; try again next frame
				return false;
			}
			data = data.cloneVertices().cloneColors();
			if (def.resizeH != 128 || def.resizeV != 128)
			{
				data.scale(def.resizeH, def.resizeV, def.resizeH);
			}
			// change every face in place; recolor() would merge faces whose new color matches an old one
			short[] colors = data.getFaceColors();
			for (int i = 0; i < colors.length; i++)
			{
				colors[i] = tint(colors[i], tint);
			}
			// spot anims are lit with ambient 64 + their own and contrast 850 + their own
			model = data.light(64 + def.ambient, 850 + def.contrast, -30, -50, -30);
			animation = client.loadAnimation(def.animation);
		}
		return true;
	}

	private RuneLiteObject spawn(Player wearer)
	{
		RuneLiteObject object = client.createRuneLiteObject();
		object.setModel(model);
		// each object has its own controller (its own frame), restarting itself whenever it finishes
		object.setAnimationController(new AnimationController(client, animation).setOnFinished(AnimationController::loop));
		object.setActive(true);
		return object;
	}

	private void place(RuneLiteObject object, Player wearer, long now)
	{
		LocalPoint at = wearer.getLocalLocation();
		if (at == null)
		{
			return;
		}
		object.setLocation(at, wearer.getWorldView().getPlane());
		if (rise > 0)
		{
			// setLocation puts it on the ground; Z grows downward, so subtract to raise it
			double climb = Math.floorMod(now, riseMillis) / (double) riseMillis;
			object.setZ(object.getZ() - (int) (rise * climb));
		}
	}
}
