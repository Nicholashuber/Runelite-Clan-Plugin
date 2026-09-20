# Assets drop folder

Nothing in this folder is packaged into the plugin. I copy the finished files into
`src/main/resources/com/corclan/` when they are ready.

## Where to put things

| What | Drop it in | Notes |
|------|-----------|-------|
| Concept art, mockups, anything big | `assets/concept/` | Git-ignored. Any format. |
| Generated art from ChatGPT | `assets/icons/src/` | 1024x1024 PNG with transparent background, named exactly as in `image-prompts.json`. Run `python assets/build_icons.py` to shrink them into the plugin. |
| Hand-made final icons | `assets/icons/` | Only if you already have them at final size: 11x11 chat icons, 16x16 panel icon, 128x128 logo. |
| Real clan chat lines | `assets/chat-examples.md` | Paste raw lines from the clan chat and the system broadcasts. Used to tune the GZ detector and broadcast parser. |

`image-prompts.json` has one ready-to-paste prompt per image, plus the shared style block.

## Chat icon filenames

Rank icons (replace the default clan rank icon next to a member's name):

```
rank_staff.png    red rhino: owner, deputy owner, administrators
rank_member.png   blue rhino: everyone else, including guests
```

Member icons (assigned to specific people in the plugin config, `rsn=key`):

```
member_crown.png    key: crown
member_trophy.png   key: trophy
member_star.png     key: star
member_skull.png    key: skull
member_gem.png      key: gem
member_fire.png     key: fire
member_founder.png  key: founder (built in for Lavasockz: the blue crown)
member_dev.png      key: dev (built in for Lavasockz: the DEV wordmark)
member_gzking.png   auto-assigned to the top GZ giver
```

`gen_placeholder_icons.py` is the script that made the current placeholders. Run `python assets/gen_placeholder_icons.py`
from the repo root to regenerate them into `./icons/`.
