from pathlib import Path
import cv2, numpy as np, json
from sklearn.svm import LinearSVC
from sklearn.metrics import classification_report, confusion_matrix
ROOT=Path(__file__).resolve().parents[1]; DATA=ROOT/'data/raw/KolektorSDD2'
# Sample 32x32 patches from each image. A patch is defective if >=1% of its mask is positive.
def get(split):
 X=[]; y=[]; names=[]
 for im in sorted((DATA/split).glob('*.png')):
  if im.stem.endswith('_GT'): continue
  mp=im.with_name(im.stem+'_GT.png')
  if not mp.exists(): continue
  g=cv2.imread(str(im),0); m=cv2.imread(str(mp),0)
  if g is None or m is None: continue
  g=cv2.resize(g,(315,115)); m=cv2.resize(m,(315,115),interpolation=cv2.INTER_NEAREST)
  for yy in range(0,84,32):
   for xx in range(0,284,32):
    a=g[yy:yy+32,xx:xx+32]; b=m[yy:yy+32,xx:xx+32]
    if a.shape!=(32,32): continue
    X.append(cv2.resize(a,(16,16)).reshape(-1).astype(np.float32)/255); y.append(int(cv2.countNonZero(b)>=10)); names.append(im.name)
 return np.asarray(X),np.asarray(y),names
X,y,_=get('train'); T,ty,names=get('test')
clf=LinearSVC(class_weight='balanced',C=.1,random_state=2026,max_iter=3000).fit(X,y)
scores=clf.decision_function(T)
# Patch prediction: any positive patch marks image defective; summarize patch-level performance.
p=scores>0
report=classification_report(ty,p,labels=[0,1],target_names=['normal_patch','defect_patch'],output_dict=True,zero_division=0)
summary={'train_patches':len(y),'positive_train_patches':int(y.sum()),'test_patches':len(ty),'positive_test_patches':int(ty.sum()),'patch_report':report,'note':'Patch metrics only; image-level aggregation still needs validation.'}
(ROOT/'reports/kolektor-patch-results.json').write_text(json.dumps(summary,indent=2),encoding='utf-8')
print(json.dumps({'train_patches':len(y),'positive_train':int(y.sum()),'test_patches':len(ty),'patch_recall':report['defect_patch']['recall'],'patch_precision':report['defect_patch']['precision']},indent=2))
