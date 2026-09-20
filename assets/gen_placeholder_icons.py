from PIL import Image, ImageDraw
import os
OUT = "icons"
GOLD=(232,178,44,255); RED=(200,50,50,255); BLUE=(70,130,220,255); GREEN=(80,190,90,255)
PURPLE=(160,80,220,255); WHITE=(240,240,240,255); GREY=(150,150,150,255); DARK=(30,30,30,255); ORANGE=(240,120,30,255)

def img(n=11): return Image.new("RGBA",(n,n),(0,0,0,0))

def crown(color):
    im=img(); d=ImageDraw.Draw(im)
    d.polygon([(1,9),(1,3),(3,6),(5,1),(7,6),(9,3),(9,9)],fill=color); d.line([(1,9),(9,9)],fill=DARK); return im
def star(color):
    im=img(); d=ImageDraw.Draw(im)
    d.polygon([(5,0),(6,3),(10,4),(7,6),(8,10),(5,8),(2,10),(3,6),(0,4),(4,3)],fill=color); return im
def trophy(color):
    im=img(); d=ImageDraw.Draw(im)
    d.rectangle([2,1,8,5],fill=color); d.rectangle([4,6,6,7],fill=color); d.rectangle([3,8,7,9],fill=color)
    d.point((1,2),fill=color); d.point((9,2),fill=color); return im
def skull(color):
    im=img(); d=ImageDraw.Draw(im)
    d.ellipse([1,0,9,8],fill=color); d.rectangle([3,7,7,10],fill=color)
    d.rectangle([3,3,4,4],fill=DARK); d.rectangle([6,3,7,4],fill=DARK); d.point((5,6),fill=DARK); return im
def gem(color):
    im=img(); d=ImageDraw.Draw(im)
    d.polygon([(2,1),(8,1),(10,4),(5,10),(0,4)],fill=color); d.line([(0,4),(10,4)],fill=WHITE); return im
def fire(color):
    im=img(); d=ImageDraw.Draw(im)
    d.polygon([(5,0),(8,4),(9,7),(7,10),(3,10),(1,7),(3,4),(4,6)],fill=color); d.polygon([(5,5),(7,8),(5,10),(3,8)],fill=GOLD); return im
def shield(color):
    im=img(); d=ImageDraw.Draw(im)
    d.polygon([(1,1),(9,1),(9,6),(5,10),(1,6)],fill=color); d.line([(5,2),(5,8)],fill=WHITE); return im
def sword(color):
    im=img(); d=ImageDraw.Draw(im)
    d.line([(9,1),(3,7)],fill=color,width=2); d.line([(2,6),(4,8)],fill=GREY,width=1); d.line([(1,9),(3,7)],fill=DARK,width=1); return im
def dot(color):
    im=img(); d=ImageDraw.Draw(im); d.ellipse([2,2,8,8],fill=color); return im

files = {
  # member override icons
  "member_crown.png": crown(GOLD), "member_trophy.png": trophy(GOLD), "member_star.png": star(GOLD),
  "member_skull.png": skull(WHITE), "member_gem.png": gem(BLUE), "member_fire.png": fire(ORANGE),
  "member_gzking.png": crown(PURPLE),
  # clan rank icons
  "rank_owner.png": crown(GOLD), "rank_deputy_owner.png": crown(GREY), "rank_administrator.png": shield(RED),
  "rank_high.png": star(BLUE), "rank_medium.png": sword(GREEN), "rank_low.png": dot(GREEN), "rank_guest.png": dot(GREY),
}
os.makedirs(OUT, exist_ok=True)
for name, im in files.items(): im.save(os.path.join(OUT,name))

# panel icon 16x16 + logo 128x128: dark circle with gold "C"
def logo(n):
    im=Image.new("RGBA",(n,n),(0,0,0,0)); d=ImageDraw.Draw(im)
    pad=max(1,n//16); d.ellipse([pad,pad,n-pad-1,n-pad-1],fill=DARK,outline=GOLD,width=max(1,n//16))
    r=n//4; c=n//2; w=max(2,n//7)
    d.arc([c-r,c-r,c+r,c+r],start=40,end=320,fill=GOLD,width=w)
    return im
logo(16).save(os.path.join(OUT,"panel_icon.png")); logo(128).save(os.path.join(OUT,"logo.png"))
print(sorted(os.listdir(OUT)))
