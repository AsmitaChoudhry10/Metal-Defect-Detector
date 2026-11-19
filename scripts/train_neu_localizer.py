from pathlib import Path
import csv,xml.etree.ElementTree as ET,cv2,numpy as np,json,random
from sklearn.svm import LinearSVC
from sklearn.metrics import classification_report
R=Path(__file__).resolve().parents[1]; C=['crazing','inclusion','patches','pitted_surface','rolled-in_scale','scratches']; ix={x:i for i,x in enumerate(C)}; H=cv2.HOGDescriptor((64,64),(16,16),(8,8),(8,8),9)
def ann(p):
 r=ET.parse(p).getroot(); return [(o.findtext('name'),*[int(o.find('bndbox').findtext(k)) for k in ('xmin','ymin','xmax','ymax')]) for o in r.findall('object')]
def f(im,b):
 x1,y1,x2,y2=b; z=im[max(y1,0):min(y2,im.shape[0]),max(x1,0):min(x2,im.shape[1])]; return H.compute(cv2.resize(z,(64,64))).ravel()
def rows(s): return list(csv.DictReader((R/'data/processed/splits'/f'{s}.csv').open(encoding='utf-8')))
rng=random.Random(2026); X=[];Y=[]
for q in rows('train'):
 im=cv2.imread(str(R/q['image']),0); bs=ann(R/q['annotation'])
 for c,x1,y1,x2,y2 in bs: X.append(f(im,(x1,y1,x2,y2)));Y.append(ix[c])
 for _ in range(2):
  for t in range(20):
   x=rng.randrange(136);y=rng.randrange(136);b=(x,y,x+64,y+64)
   if all(max(0,min(b[2],x2)-max(b[0],x1))*max(0,min(b[3],y2)-max(b[1],y1))<.05*64*64 for _,x1,y1,x2,y2 in bs): X.append(f(im,b));Y.append(6);break
m=LinearSVC(C=.1,class_weight='balanced',max_iter=5000,random_state=2026).fit(np.asarray(X,np.float32),Y)
ys=[];ps=[];hit=tot=0
for q in rows('test'):
 im=cv2.imread(str(R/q['image']),0); bs=ann(R/q['annotation'])
 for c,x1,y1,x2,y2 in bs: ys.append(ix[c]);ps.append(m.predict(f(im,(x1,y1,x2,y2))[None,:])[0])
 for c,x1,y1,x2,y2 in bs:
  tot+=1; found=False
  for sz in (32,48,64,96):
   for y in range(0,201-sz,32):
    for x in range(0,201-sz,32):
     if m.predict(f(im,(x,y,x+sz,y+sz))[None,:])[0]==ix[c]:
      inter=max(0,min(x+sz,x2)-max(x,x1))*max(0,min(y+sz,y2)-max(y,y1)); union=sz*sz+(x2-x1)*(y2-y1)-inter
      if inter/max(1,union)>=.3: found=True; break
    if found:break
   if found:break
  hit+=found
rep=classification_report(ys,ps,labels=list(range(6)),target_names=C,output_dict=True,zero_division=0)
out={'model':'HOG sliding-window SVM localization baseline','train_patches':len(Y),'gt_box_class_accuracy':float(np.mean(np.array(ys)==np.array(ps))),'test_gt_boxes':tot,'localization_recall_iou_0.3':hit/max(1,tot),'per_class_gt_crop_report':rep,'warning':'Exploratory classical baseline; box recall must be assessed before app use.'}
(R/'reports/neu-localizer-results.json').write_text(json.dumps(out,indent=2)); (R/'models').mkdir(exist_ok=True); import joblib; joblib.dump(m,R/'models/neu_localizer_svm.joblib'); print(out['gt_box_class_accuracy'],out['localization_recall_iou_0.3'],tot)
