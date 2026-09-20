# Assets drop folder

Nothing in this folder is packaged into the plugin. I copy the finished files into
`src/main/resources/com/corclan/` when they are ready.

## Where to put things

| What | Drop it in | Notes |
|------|-----------|-------|
| Concept art, mockups, anything big | `assets/concept/` | Git-ignored. Any format. |
| Finished chat icons | `assets/icons/` | PNG with transparency, **11x11 px** (that is the size OSRS draws chat icons at). Use the filenames below. |
| Panel icon | `assets/icons/panel_icon.png` | 16x16 PNG, shows in the RuneLite sidebar. |
| Clan logo | `assets/icons/logo.png` | 128x128 PNG, shows at the top of the panel. |
| Real clan chat lines | `assets/chat-examples.md` | Paste raw lines from the clan chat and the system broadcasts. Used to tune the GZ detector and broadcast parser. |

## Chat icon filenames

Rank icons (replace the default clan rank icon next to a member's name):

```
rank_owner.png
rank_deputy_owner.png
rank_administrator.png
rank_high.png          (any rank above "medium" that is not admin/owner)
rank_medium.png
rank_low.png           (recruit / lowest ranks)
rank_guest.png
```

Member icons (assigned to specific people in the plugin config, `rsn=key`):

```
member_crown.png    key: crown
member_trophy.png   key: trophy
member_star.png     key: star
member_skull.png    key: skull
member_gem.png      key: gem
member_fire.png     key: fire
member_gzking.png   auto-assigned to the top GZ giver
```

`gen_placeholder_icons.py` is the script that made the current placeholders. Run `python assets/gen_placeholder_icons.py`
from the repo root to regenerate them into `./icons/`.
