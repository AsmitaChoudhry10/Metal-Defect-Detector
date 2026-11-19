from pathlib import Path
import json
import cv2
import numpy as np
from sklearn.metrics import accuracy_score, classification_report, confusion_matrix
ROOT=Path(__file__).resolve().parents[1]
DATA=ROOT/'data/raw/KolektorSDD2'
CLASSES=['normal','defective']
HOG=cv2.HOGDescriptor((200,200),(40,40),(20,20),(20,20),9)
def load(split):
 X=[]; y=[]; skipped=[]
 for image in sorted((DATA/split).glob('*.png')):
  if image.stem.endswith('_GT'): continue
  mask=image.with_name(image.stem+'_GT.png')
  if not mask.is_file(): skipped.append(image.name); continue
  gray=cv2.imread(str(image),cv2.IMREAD_GRAYSCALE)
  gt=cv2.imread(str(mask),cv2.IMREAD_GRAYSCALE)
  if gray is None or gt is None: skipped.append(image.name); continue
  label=int(cv2.countNonZero(gt)>0)
  resized=cv2.resize(gray,(200,200),interpolation=cv2.INTER_AREA)
  X.append(HOG.compute(resized).reshape(-1)); y.append(label)
 return np.asarray(X,dtype=np.float32),np.asarray(y,dtype=np.int32),skipped
X,y,sk_train=load('train'); T,ty,sk_test=load('test')
svm=cv2.ml.SVM_create(); svm.setType(cv2.ml.SVM_C_SVC); svm.setKernel(cv2.ml.SVM_LINEAR); svm.setC(1.0)
svm.setClassWeights(np.array([[1.0],[max(1.0,float((y==0).sum()/(y==1).sum()))]],dtype=np.float32))
svm.train(X,cv2.ml.ROW_SAMPLE,y)
_,raw=svm.predict(T); pred=raw.reshape(-1).astype(np.int32)
report=classification_report(ty,pred,labels=[0,1],target_names=CLASSES,output_dict=True,zero_division=0)
model_dir=ROOT/'android-app/app/src/main/assets'; model_dir.mkdir(parents=True,exist_ok=True)
svm.save(str(model_dir/'kolektor_normal_defect_svm.xml'))
summary={'model':'OpenCV HOG + class-weighted linear SVM','input_resize':[200,200],'class_id_order':CLASSES,'train_count':len(y),'train_counts':{'normal':int((y==0).sum()),'defective':int((y==1).sum())},'test_count':len(ty),'test_counts':{'normal':int((ty==0).sum()),'defective':int((ty==1).sum())},'test_accuracy':float(accuracy_score(ty,pred)),'test_report':report,'confusion_matrix_labels':CLASSES,'confusion_matrix':confusion_matrix(ty,pred,labels=[0,1]).tolist(),'skipped_unpaired_train_images':sk_train,'skipped_unpaired_test_images':sk_test,'warning':'Only applies to KolektorSDD2 product imagery; not interchangeable with NEU-DET or plumbing fitting images.'}
(ROOT/'reports/kolektor-binary-results.json').write_text(json.dumps(summary,indent=2),encoding='utf-8')
print(json.dumps({k:v for k,v in summary.items() if k!='test_report'},indent=2))
