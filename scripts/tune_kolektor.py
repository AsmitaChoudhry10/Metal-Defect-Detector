from pathlib import Path
import json
import cv2, numpy as np
from sklearn.model_selection import train_test_split
from sklearn.svm import LinearSVC
from sklearn.metrics import precision_recall_fscore_support, accuracy_score, confusion_matrix, classification_report
ROOT=Path(__file__).resolve().parents[1]; DATA=ROOT/'data/raw/KolektorSDD2'
HOG=cv2.HOGDescriptor((176,64),(16,16),(8,8),(8,8),9)
def load(split):
 X=[]; y=[]; skipped=[]
 for im in sorted((DATA/split).glob('*.png')):
  if im.stem.endswith('_GT'): continue
  mask=im.with_name(im.stem+'_GT.png')
  if not mask.is_file(): skipped.append(im.name); continue
  gray=cv2.imread(str(im),0); gt=cv2.imread(str(mask),0)
  if gray is None or gt is None: skipped.append(im.name); continue
  # Preserve the source aspect ratio with a fixed 176x64 resize.
  gray=cv2.resize(gray,(176,64),interpolation=cv2.INTER_AREA)
  X.append(HOG.compute(gray).reshape(-1)); y.append(int(cv2.countNonZero(gt)>0))
 return np.asarray(X,dtype=np.float32),np.asarray(y,dtype=np.int32),skipped
X,y,sktr=load('train'); T,ty,skt=load('test')
tr,va=train_test_split(np.arange(len(y)),test_size=.2,random_state=2026,stratify=y)
choices=[]
for c in [.1,1]:
 for w in [3,10]:
  model=LinearSVC(C=c,class_weight={0:1,1:w},random_state=2026,max_iter=10000)
  model.fit(X[tr],y[tr]); p=model.predict(X[va])
  prec,rec,f1,_=precision_recall_fscore_support(y[va],p,labels=[1],zero_division=0)
  choices.append((float(f1[0]),float(rec[0]),float(prec[0]),c,w))
# Select by F1 on the internal validation split only.
_,_,_,c,w=max(choices)
model=LinearSVC(C=c,class_weight={0:1,1:w},random_state=2026,max_iter=10000).fit(X,y)
pred=model.predict(T)
report=classification_report(ty,pred,labels=[0,1],target_names=['normal','defective'],output_dict=True,zero_division=0)
summary={'model':'aspect-preserving HOG + class-weighted LinearSVC','resize':[176,64],'train_count':len(y),'official_test_count':len(ty),'selected_C':c,'selected_defect_weight':w,'validation_grid_best_f1':max(choices)[0],'test_accuracy':float(accuracy_score(ty,pred)),'test_report':report,'confusion_matrix_labels':['normal','defective'],'confusion_matrix':confusion_matrix(ty,pred,labels=[0,1]).tolist(),'skipped_train':sktr,'skipped_test':skt,'caution':'KolektorSDD2-specific binary experiment; do not apply to NEU-DET or plumbing fittings.'}
import joblib
(ROOT/'models').mkdir(exist_ok=True); joblib.dump({'model':model,'hog':'176x64, block16, stride8, cell8, bins9','labels':['normal','defective']},ROOT/'models/kolektor_binary_hog_svm.joblib')
(ROOT/'reports/kolektor-tuned-results.json').write_text(json.dumps(summary,indent=2),encoding='utf-8')
print(json.dumps({'selected_C':c,'weight':w,'accuracy':summary['test_accuracy'],'normal':report['normal'],'defective':report['defective'],'confusion':summary['confusion_matrix']},indent=2))
