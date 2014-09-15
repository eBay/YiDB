#This is readme file for CMS_DAL (cms data access layer)

#Issues:
2012/5/17: DELETE
we never do real deletion, we only mark as deleted, when we delete root documemnt, we only mark
root document as deleted, when access to embed document whose root document marks as deleted, we need
special handling

2012/5/17: optimistic lock, version check
if input entity has version, we need to check the input version == saved version, then increase version by one
if input entity does not have version, increase version by one

2012/5/17: reference map support

2012/5/18: for mandatory field, you can not remove it, you can not set it to null/empty; 
even for optional field, should we apply value constraints  

2012/5/18: how to invalidate reference if the referenced object has been removed

2012/5/18: connection management